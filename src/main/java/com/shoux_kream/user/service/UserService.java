package com.shoux_kream.user.service;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.shoux_kream.config.jwt.impl.AuthTokenImpl;
import com.shoux_kream.config.jwt.impl.JwtProviderImpl;
import com.shoux_kream.exception.InvalidPasswordException;
import com.shoux_kream.user.dto.JwtTokenDto;
import com.shoux_kream.user.dto.request.AccountRequest;
import com.shoux_kream.user.dto.request.JwtTokenLoginRequest;
import com.shoux_kream.user.dto.request.UserRequest;
import com.shoux_kream.user.dto.response.UserAddressDto;
import com.shoux_kream.user.dto.response.UserResponse;
import com.shoux_kream.user.entity.RefreshToken;
import com.shoux_kream.user.entity.Role;
import com.shoux_kream.user.entity.User;
import com.shoux_kream.user.entity.UserAddress;
import com.shoux_kream.user.repository.RefreshTokenRepository;
import com.shoux_kream.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtProviderImpl jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    //TODO 초기값 중복 init 문제, mysql은 인메모리 DB가 아니라 unique 중복값 문제가 존재함
//    @jakarta.annotation.PostConstruct
//    public void init() {
//        User user = User.builder()
//                .email("1@1")
//                .password(bCryptPasswordEncoder.encode("1"))
//                .name("elice")
//                .nickname("e")
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .role(Role.USER)
//                .build();
//        userRepository.save(user);
//    }

    //회원가입
    public Long signup(UserRequest dto) {
        return userRepository.save(User.builder()
                .password(bCryptPasswordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .name(dto.getName())
                .nickname(dto.getNickname())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .role(Role.USER)
                .build()).getId();
    }

    //회원 조회
    public UserResponse getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }

    // 회원정보 수정
    public void updateProfile(AccountRequest dto) {
        Long userId = getUser().getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        // 기존 비밀번호 입력하지 않음 or 입력한 기존 비밀번호와 db 현재 비밀번호가 일치하지 않음
        if (dto.getPassword() == null || !bCryptPasswordEncoder.matches(dto.getPassword(), user.getPassword())) {
           log.info(dto.getPassword());
           log.info(user.getPassword());
            throw new InvalidPasswordException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 새 비밀번호가 null or 비어있을 때 기존 비밀번호 유지 / 아니라면 비밀번호 업데이트
        String encodedNewPassword = user.getPassword();
        if (dto.getNewPassword() != null && !dto.getNewPassword().isEmpty()) {
            encodedNewPassword = bCryptPasswordEncoder.encode(dto.getNewPassword());
        }

        User updatedUser = User.builder()
                .id(user.getId())
                .email(dto.getEmail())
                .name(dto.getName())
                .nickname(dto.getNickname())
                .createdAt(user.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .role(user.getRole())
                .addresses(user.getAddresses())
                .password(encodedNewPassword)
                .build();

        userRepository.save(updatedUser);
    }


    //회원정보 삭제
    public void deleteUser() {
        Long userId = getUser().getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));
        userRepository.delete(user);
    }

    //로그인
    public JwtTokenDto login(JwtTokenLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!bCryptPasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", user.getId());
        claims.put("role", user.getRole());

        String sub = request.getEmail();
        AuthTokenImpl accessToken = jwtProvider.createAccessToken(
                sub,
                user.getRole(),
                claims
        );

        AuthTokenImpl refreshToken = jwtProvider.createRefreshToken(
                sub,
                user.getRole(),
                claims
        );

        String jti = refreshToken.getDate().getId();
        RefreshToken token = RefreshToken.builder().refreshToken(refreshToken.getToken()).email(request.getEmail()).jti(jti).build();
        refreshTokenRepository.save(token);

        return JwtTokenDto.builder()
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .build();
    }


    public List<UserAddressDto> getUserAddresses(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("user doesn't exist"));
        //optional 예외처리 적용

        List<UserAddress> userAddresses = user.getAddresses();

        return userAddresses.stream()
                .map(UserAddress -> new UserAddressDto(UserAddress))
                .collect(Collectors.toList());
    }
}


