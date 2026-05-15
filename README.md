백엔드 SpringBoot + MySQL 폴더 추가.  
프론트 분들은 android 폴더 사용.  
백엔드 서버 사용할 때 application.properties에서 password 자신의 MySQL 비밀번호로 변경해야 합니다(아직 공유 안함).  
솔직히 수업 때 배운게 거의 없어서 대부분 AI로 만들었습니다. 참고 바랍니다.

Mysql 스키마
```sql
-- 1. 유저 테이블 (구글 로그인 및 마이페이지 필드 추가)
CREATE TABLE `User` (  
    `userNumber` int NOT NULL AUTO_INCREMENT,
    `firebaseUid` varchar(255) NOT NULL,          -- 구글 로그인 연동 고유 키
    `userName` varchar(255) NULL,
    `userNickname` varchar(255) NULL,             -- 마이페이지 별명
    `userEmail` varchar(255) NULL,
    `userProfileImage` varchar(1000) NULL,        -- 프로필 사진 주소
    `userBio` varchar(255) NULL,                  -- 자기소개
    `userGender` varchar(10) NULL,
    `userBirth` date NULL,
    `userTemperature` double DEFAULT 36.5,        -- 기본 매너 온도
    PRIMARY KEY (`userNumber`),
    UNIQUE (`firebaseUid`)                        -- 구글 ID 중복 방지
);

-- 2. 장소 테이블 (기존 유지)
CREATE TABLE `Place` (
    `placeNumber` int NOT NULL,
    `placeName` varchar(255) NULL,
    `placeAddress` varchar(255) NULL,
    `placeCategory` varchar(255) NULL,
    `placeContent` varchar(1000) NULL,
    `placeInfo` varchar(1000) NULL,
    `placeTag` varchar(255) NULL,
    `placeLongitude` double NULL,
    `placeLatitude` double NULL,
    `placeReviewCount` int NULL,
    PRIMARY KEY (`placeNumber`)
);

-- 3. 커뮤니티 게시판 (기존 유지)
CREATE TABLE `Community` (
    `communityNumber` int NOT NULL AUTO_INCREMENT,
    `userNumber` int NOT NULL,
    `communityTitle` varchar(255) NULL,
    `communityContent` varchar(2000) NULL,
    `communityDate` date NULL,
    `communityCategory` varchar(255) NULL,
    PRIMARY KEY (`communityNumber`),
    FOREIGN KEY (`userNumber`) REFERENCES `User`(`userNumber`)
);

-- 4. 커뮤니티 이미지 (기존 유지)
CREATE TABLE `CommunityImage` (
    `imageNumber` int NOT NULL AUTO_INCREMENT,
    `communityNumber` int NOT NULL,
    `imageCreateDate` date NULL,
    `imageSize` int NULL,
    `imageOriginalName` varchar(255) NULL,
    `imageStoredName` varchar(255) NULL,
    PRIMARY KEY (`imageNumber`),
    FOREIGN KEY (`communityNumber`) REFERENCES `Community`(`communityNumber`)
);

-- 5. 플래너 (기존 유지)
CREATE TABLE `Planner` (
    `plannerNumber` int NOT NULL AUTO_INCREMENT,
    `userNumber` int NOT NULL,
    `firstDate` date NULL,
    `lastDate` date NULL,
    `plannerTitle` varchar(255) NULL,
    `plannerHit` int NULL,
    PRIMARY KEY (`plannerNumber`),
    FOREIGN KEY (`userNumber`) REFERENCES `User`(`userNumber`)
);

-- 6. 여행 계획 세부사항 (기존 유지)
CREATE TABLE `Plan` (
    `planNumber` int NOT NULL AUTO_INCREMENT,
    `plannerNumber` int NOT NULL,
    `planDate` date NULL,
    `placeLongitude` double NULL,
    `placeLatitude` double NULL,
    `planTime` time NULL,
    `planMemo` varchar(500) NULL,
    `placeName` varchar(255) NULL,
    `planOrder` int NULL,
    PRIMARY KEY (`planNumber`),
    FOREIGN KEY (`plannerNumber`) REFERENCES `Planner`(`plannerNumber`)
);

-- 7. 리뷰 (기존 유지)
CREATE TABLE `review` (
    `reviewNumber` int NOT NULL AUTO_INCREMENT,
    `placeNumber` int NOT NULL,
    `userNumber` int NOT NULL,
    `reviewContent` varchar(1000) NULL,
    `reviewRating` int NULL,
    `reviewDate` date NULL,
    PRIMARY KEY (`reviewNumber`),
    FOREIGN KEY (`placeNumber`) REFERENCES `Place`(`placeNumber`),
    FOREIGN KEY (`userNumber`) REFERENCES `User`(`userNumber`)
);

-- 8. 가계부 (기존 유지)
CREATE TABLE `Budget` (
    `budgetNumber` int NOT NULL AUTO_INCREMENT,
    `plannerNumber` int NOT NULL,
    `budgetTitle` varchar(255) NULL,
    `budgetAmount` double NULL,
    `budgetCategory` varchar(255) NULL,
    `budgetDate` date NULL,
    `budgetType` varchar(10) NULL,
    PRIMARY KEY (`budgetNumber`),
    FOREIGN KEY (`plannerNumber`) REFERENCES `Planner`(`plannerNumber`)
);

-- 9. 팔로우 (신규 추가)
CREATE TABLE `Follow` (
    `followNumber` int NOT NULL AUTO_INCREMENT,
    `followerNumber` int NOT NULL,                -- 나
    `followingNumber` int NOT NULL,               -- 상대방
    `followDate` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`followNumber`),
    FOREIGN KEY (`followerNumber`) REFERENCES `User`(`userNumber`),
    FOREIGN KEY (`followingNumber`) REFERENCES `User`(`userNumber`),
    UNIQUE (`followerNumber`, `followingNumber`)  -- 중복 팔로우 방지 복합 유니크
);

-- 10. 스크랩 (신규 추가)
CREATE TABLE `Scrap` (
    `scrapNumber` int NOT NULL AUTO_INCREMENT,
    `userNumber` int NOT NULL,
    `communityNumber` int NOT NULL,
    `scrapDate` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`scrapNumber`),
    FOREIGN KEY (`userNumber`) REFERENCES `User`(`userNumber`),
    FOREIGN KEY (`communityNumber`) REFERENCES `Community`(`communityNumber`),
    UNIQUE (`userNumber`, `communityNumber`)      -- 중복 스크랩 방지
);

-- 11. 커뮤니티 좋아요 (신규 추가)
CREATE TABLE `CommunityLike` (
    `likeNumber` int NOT NULL AUTO_INCREMENT,
    `userNumber` int NOT NULL,
    `communityNumber` int NOT NULL,
    `likeDate` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`likeNumber`),
    FOREIGN KEY (`userNumber`) REFERENCES `User`(`userNumber`),
    FOREIGN KEY (`communityNumber`) REFERENCES `Community`(`communityNumber`),
    UNIQUE (`userNumber`, `communityNumber`)      -- 중복 좋아요 방지
);
