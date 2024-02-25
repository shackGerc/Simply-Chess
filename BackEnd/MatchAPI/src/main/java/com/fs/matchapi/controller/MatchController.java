package com.fs.matchapi.controller;

import com.fs.matchapi.dtos.ConnectRequest;
import com.fs.matchapi.dtos.GameplayRequest;
import com.fs.matchapi.dtos.MatchDto;
import com.fs.matchapi.dtos.MatchWithPlayerTeam;
import com.fs.matchapi.dtos.PlayerInQueueResponse;
import com.fs.matchapi.dtos.PromoteRequest;
import com.fs.matchapi.exceptions.IllegalMovementException;
import com.fs.matchapi.exceptions.PieceNotFoundException;
import com.fs.matchapi.model.Player;
import com.fs.matchapi.service.MatchService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@AllArgsConstructor
@Slf4j
@RestController
@RequestMapping("/match")
@CrossOrigin(origins = "http://localhost:4200")
public class MatchController {

    private MatchService matchService;
    private SimpMessagingTemplate simpMessagingTemplate;

    @PostMapping("/create")
    public ResponseEntity<MatchWithPlayerTeam> createGame(@RequestBody Player request) {
        log.info("start game request: {}", request);
        MatchWithPlayerTeam match = matchService.createMatch(request);

        return ResponseEntity.ok(match);
    }

    @PutMapping("/connect")
    public ResponseEntity<MatchWithPlayerTeam> connect(@RequestBody ConnectRequest request) {
        log.info("connect request: {}", request);
        MatchWithPlayerTeam match =
                matchService.connectMatchById(request.getPlayer(), UUID.fromString(request.getGameId()));

        simpMessagingTemplate.convertAndSend("/queue/game-progress/" + match.getMatch().getId(),
                match.getMatch());

        return ResponseEntity.ok(match);
    }

    @PostMapping("/connect/matchesQueue")
    public ResponseEntity<PlayerInQueueResponse> connectToMatchesQueue(@RequestBody Player request) {
        log.info("connect request: {}", request);
        return ResponseEntity.ok(matchService.enqueueForMatch(request));
    }

    @PutMapping("/gameplay")
    public ResponseEntity<MatchDto> gamePlay(@RequestBody GameplayRequest request)
            throws IllegalMovementException, PieceNotFoundException {
        log.info("gameplay: {}", request);
        MatchDto match = matchService.move(request.getPlayer(),
                UUID.fromString(request.getMatchId()), request.getPieceToMove(), request.getTarget());

        simpMessagingTemplate.convertAndSend("/queue/game-progress/" + match.getId(), match);

        return ResponseEntity.ok(match);
    }

    @PutMapping("/promote")
    public ResponseEntity<MatchDto> promote(@RequestBody PromoteRequest request)
            throws IllegalMovementException, PieceNotFoundException {
        log.info("Promote: {}", request);
        MatchDto match = matchService.promoteAPawn(request.getPlayer(), UUID.fromString(request.getMatchId()),
                request.getPawnToPromote(),
                request.getNewPieceSymbol());

        simpMessagingTemplate.convertAndSend("/queue/game-progress/" + match.getId(), match);

        return ResponseEntity.ok(match);
    }

    @PutMapping("/tieMatch")
    public ResponseEntity<MatchDto> tieMatch(@PathVariable String matchId) {
        log.info("Tie match: {}", matchId);
        MatchDto match = matchService.setMatchAsTied(UUID.fromString(matchId));

        simpMessagingTemplate.convertAndSend("/queue/game-progress/" + match.getId(), match);

        return ResponseEntity.ok(match);
    }
}
