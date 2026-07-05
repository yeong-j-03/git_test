package com.dormmatch.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ── 400 ──
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다."),
    BIO_TOO_LONG(HttpStatus.BAD_REQUEST, "자기소개는 100자를 초과할 수 없습니다."),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "JPG/PNG 형식의 이미지만 업로드 가능합니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 5MB를 초과할 수 없습니다."),
    PAST_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "과거 날짜는 설정할 수 없습니다."),

    // ── 401 ──
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),

    // ── 403 ──
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
    CERTIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "기숙사 인증이 필요합니다."),
    SURVEY_REQUIRED(HttpStatus.FORBIDDEN, "설문 작성이 필요합니다."),
    NOT_CHAT_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 채팅방의 참여자가 아닙니다."),
    NOT_MATCH_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 매칭의 당사자가 아닙니다."),

    // ── 404 ──
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    CERTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."),
    SURVEY_NOT_FOUND(HttpStatus.NOT_FOUND, "설문 내역을 찾을 수 없습니다."),
    MATCH_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 요청을 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),

    // ── 409 ──
    DETAILS_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 필수 정보가 등록된 상태입니다."),
    CERTIFICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 인증 요청이 존재합니다."),
    CERTIFICATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 인증 건입니다."),
    SURVEY_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 설문을 제출한 상태입니다."),
    HEART_ALREADY_SENT(HttpStatus.CONFLICT, "이미 해당 유저에게 하트를 보냈습니다."),
    ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정한 상태입니다."),

    // ── 422 ──
    NOT_ACCEPTED_STATUS(HttpStatus.UNPROCESSABLE_ENTITY, "ACCEPTED 상태에서만 확정할 수 있습니다."),

    // ── 500 ──
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    KAKAO_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 API 연동 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
