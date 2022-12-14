package com.sparta.doing.service;

import com.sparta.doing.controller.dto.BoardDto;
import com.sparta.doing.controller.requestdto.BoardRequestDto;
import com.sparta.doing.controller.requestdto.PostRequestDto;
import com.sparta.doing.controller.responsedto.BoardResponseDto;
import com.sparta.doing.entity.Board;
import com.sparta.doing.entity.BoardLike;
import com.sparta.doing.entity.PostEntity;
import com.sparta.doing.entity.UserEntity;
import com.sparta.doing.entity.constant.SearchType;
import com.sparta.doing.exception.BoardNotFoundException;
import com.sparta.doing.repository.BoardLikeRepository;
import com.sparta.doing.repository.BoardRepository;
import com.sparta.doing.repository.UserRepository;
import com.sparta.doing.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
@Service
public class BoardService {
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;

    @Transactional(readOnly = true)
    public Page<BoardDto> searchBoards(SearchType searchType,
                                       String searchKeyword,
                                       Pageable pageable) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return boardRepository.findAll(pageable).map(BoardDto::from);
        }
        if (searchType.equals(SearchType.TITLE)) {
            return boardRepository.findByBoardTitleContaining(searchKeyword, pageable).map(BoardDto::from);
        }
        if (searchType.equals(SearchType.CONTENT)) {
            return boardRepository.findByBoardContentContaining(searchKeyword, pageable).map(BoardDto::from);
        }
        if (searchType.equals(SearchType.ID)) {
            return boardRepository.findByUserEntity_UsernameContaining(searchKeyword, pageable).map(BoardDto::from);
        }
        if (searchType.equals(SearchType.NICKNAME)) {
            return boardRepository.findByUserEntity_NicknameContaining(searchKeyword, pageable).map(BoardDto::from);
        }
        if (searchType.equals(SearchType.HASHTAG)) {
            return boardRepository.findByBoardHashtagContaining("#" + searchKeyword, pageable).map(BoardDto::from);
        }
        return boardRepository.findAll(pageable).map(BoardDto::from);
    }

    public BoardResponseDto createBoard(BoardRequestDto boardRequestDto, Long userId) {
        UserEntity foundUserEntity = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("????????? ?????? ????????? ????????????."));

        Board createdBoard = Board.builder()
                .boardTitle(boardRequestDto.getBoardTitle())
                .authorName(foundUserEntity.getNickname())
                .boardContent(boardRequestDto.getBoardContent())
                .boardHashtag(boardRequestDto.getBoardHashtag())
                .build();

        createdBoard.mapToUserEntity(foundUserEntity);

        return BoardResponseDto.from(boardRepository.save(createdBoard));
    }

    // ?????? ????????? ?????? ??????
    public BoardResponseDto getOneBoardWithComments(Long boardId) {
        // db?????? ????????? ??????
        Board getOneBoard = boardRepository.findById(boardId)
                .orElseThrow(
                        () -> new BoardNotFoundException(
                                "?????? ???????????? ???????????? ????????????."));
        // ????????? ??????
        getOneBoard.visit();
        // ??????
        return BoardResponseDto.from(getOneBoard);
    }

    public void updateBoard(Long boardId, BoardRequestDto
            boardRequestDto, Long userId) {
        // ??????????????? ????????? ??????
        Board foundBoardToUpdate = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("?????? ???????????? ???????????? ????????????."));
        // ????????? ???????????? ?????? ???????????? ????????? userId??? ???????????? ??????????????? ??????
        if (!Objects.equals(foundBoardToUpdate.getUserEntity().getId(), SecurityUtil.getCurrentUserIdByLong())) {
            throw new BoardNotFoundException("????????? ????????? ???????????? ????????? ???????????????.");
        }
    }

    public void deleteBoard(Long boardId, Long userId) {
        // ??????????????? ????????? ??????
        Board foundBoardToDelete = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("?????? ???????????? ???????????? ????????????."));
        // ????????? ???????????? ?????? ???????????? ????????? userId??? ???????????? ??????????????? ??????
        if (!Objects.equals(foundBoardToDelete.getUserEntity().getId(), SecurityUtil.getCurrentUserIdByLong())) {
            throw new BoardNotFoundException("????????? ????????? ???????????? ????????? ???????????????.");
        }
    }

    public void boardLike(Long boardId, String userId) {
        Board foundBoardToLike = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("?????? ???????????? ???????????? ????????????."));
        UserEntity foundUserEntity = userRepository.findByUsername(userId)
                .orElseThrow(() -> new UsernameNotFoundException("????????? ?????? ????????? ????????????."));

        Optional<BoardLike> boardLikeFoundInRepo = boardLikeRepository.findByBoardAndUserEntity(foundBoardToLike, foundUserEntity);

        boardLikeFoundInRepo.ifPresentOrElse(
                boardLike -> {
                    foundBoardToLike.discountLike(boardLike);
                    foundBoardToLike.updateLikeCount();
                    boardLikeRepository.delete(boardLike);
                },
                () -> {
                    BoardLike boardLike = BoardLike.builder().build();
                    boardLike.mapToBoard(foundBoardToLike);
                    boardLike.mapToUserEntity(foundUserEntity);
                    foundBoardToLike.updateLikeCount();
                    boardLikeRepository.save(boardLike);
                }
        );
//        if(boardLikeFoundInRepo.isPresent()){
//            foundBoardToLike.discountLike(boardLikeFoundInRepo.get());
//            foundBoardToLike.updateLikeCount();
//            boardLikeRepository.delete(boardLikeFoundInRepo.get());
//        } else {
//            boardLike.mapToContent(foundBoardToLike);
//            boardLike.mapToUser(foundUserEntity);
//            foundBoardToLike.updateLikeCount();
//            boardLikeRepository.save(boardLike);
//        }

    }

    public BoardResponseDto createPost(Long boardId, PostRequestDto postRequestDto) {
        var board = boardRepository.findById(boardId)
                .orElseThrow(
                        () -> new BoardNotFoundException(
                                boardId + ": ?????? ???????????? ?????? ??? ????????????.")
                );
        if (!Objects.equals(board.getUserEntity().getId(), SecurityUtil.getCurrentUserIdByLong())) {
            throw new BoardNotFoundException("????????? ????????? ??????????????? ??? ????????? ???????????????.");
        }

        board.mapToPost(PostEntity.of(postRequestDto, board));

        return BoardResponseDto.from(board);
    }

    // public List<PostResponseDto> getPosts(Long boardId) {
    //     var board = boardRepository.findById(boardId)
    //             .orElseThrow(
    //                     () -> new BoardNotFoundException(
    //                             boardId + ": ?????? ???????????? ?????? ??? ????????????."));
    //     return
    // }
}
