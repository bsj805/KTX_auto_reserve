package org.prac.korailreserve;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "사용자", description = "코레일 티켓 예매 관련 API")
public class TicketController {
    private final TicketService ticketService;
    @Autowired
    private EmailService emailService;

    @Value("${app.email.recipient}") // Matches the path in application.yml
    private String defaultRecipientEmail; // Or just 'recipientEmail' if you prefer

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/ticket/reserve")
    @Operation(summary = "티켓 예매", description = "코레일 티켓을 예매합니다.",
            responses = {
                    @ApiResponse(description = "성공적으로 예매됨", responseCode = "200", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(description = "예매 실패", responseCode = "400", content = @Content)
            })
    public ResponseEntity<String> korail(
            @Parameter(description = "KORAIL 회원번호") @RequestParam String txtMember,
            @Parameter(description = "비밀번호") @RequestParam String txtPwd,
            @Parameter(description = "출발역 (EX. 평택)") @RequestParam String txtGoStart,
            @Parameter(description = "도착역 (EX. 서울)") @RequestParam String txtGoEnd,
            @Parameter(description = "선택 월(01~12)") @RequestParam String selMonth,
            @Parameter(description = "선택 일(01~31) (01처럼 0붙이기)") @RequestParam String selDay,
            @Parameter(description = "기차 출발 시간(시)") @RequestParam Integer startHour,
            @Parameter(description = "기차 출발 시간(분)") @RequestParam Integer startMin,
            @Parameter(description = "기차 도착 시간(시)") @RequestParam Integer endHour,
            @Parameter(description = "기차 도착 시간(분)") @RequestParam Integer endMin) {

        // Set date formatting
        LocalDate date = LocalDate.now();
        selMonth = String.format("%02d", Integer.parseInt(selMonth));
        selDay = String.format("%02d", Integer.parseInt(selDay));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime selectedDateTime = LocalDateTime.parse(date.getYear() + "-" + selMonth + "-" + selDay + " " + String.format("%02d", startHour) + ":" + String.format("%02d", startMin), formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(date.getYear() + "-" + selMonth + "-" + selDay + " " + String.format("%02d", endHour) + ":" + String.format("%02d", endMin), formatter);//        LocalTime endTime = endDateTime.toLocalTime();  // -> 09:00

//        LocalTime arrTime = LocalTime.parse("10:38");
//
//

        String subject = "코레일 기차 예약 시작";
        String body = "코레일 기차 예약이 시작되었습니다.\n\n" +
                "출발역: " + txtGoStart + "\n" +
                "도착역: " + txtGoEnd + "\n" +
                "선택 월: " + selMonth + "\n" +
                "선택 일: " + selDay + "\n" +
                "시작 시간: " + String.format("%02d", startHour)  + "\n" ;

        emailService.sendSimpleEmail(defaultRecipientEmail, subject, body);
        if (selectedDateTime.isBefore(LocalDateTime.now()) || endDateTime.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("선택한 일시가 현재보다 이전입니다.");
        }

        return ResponseEntity.ok(ticketService.reserveTicket(txtMember, txtPwd, txtGoStart, txtGoEnd, selMonth, selDay, startHour, startMin, endHour, endMin));

    }
}
