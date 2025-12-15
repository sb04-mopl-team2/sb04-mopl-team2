# 모두의 플리

[![codecov](https://codecov.io/gh/sb04-mopl-team2/sb04-mopl-team2/graph/badge.svg?token=F3N6H5J7OG)](https://codecov.io/gh/sb04-mopl-team2/sb04-mopl-team2)

# 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [팀원](#팀원)
3. [기술 스택](#기술-스택)
4. [시스템 아키텍쳐](#시스템-아키텍쳐)
5. [주요 기능](#주요-기능)
   - [콘텐츠 관리](#콘텐츠-관리)
   - [플레이리스트](#플레이리스트)
   - [소셜 기능](#소셜-기능)
   - [실시간 기능](#실시간-기능)
   - [사용자 관리](#사용자-관리)
   - [관리자 기능](#관리자-기능)
   - [주요 API 엔드포인트](#주요-api-엔드포인트)

# 프로젝트 소개

## 🌟 모두의 플리

**대규모 트래픽이 예상되는 글로벌 컨텐츠 평점 플랫폼**

모두의 플리는 영화, 드라마, 스포츠 등 다양한 콘텐츠를 평가하고, 자신만의 기준으로 플레이리스트를 만들고 공유할 수 있는 콘텐츠 큐레이션 플랫폼입니다. 실시간 같이 보기, 플레이리스트 구독, 팔로우, DM 등 소셜 기능을 통해 콘텐츠 감상의 즐거움을 다른 사람과 함께 나눌 수 있어요.

**당신만의 취향이 모여, 모두의 취향이 됩니다.** ✨

### 📅 프로젝트 정보

- **프로젝트 기간**: 2025.11.10 ~ 2025.12.18
- **팀 구성**: 6명 (Backend 개발)

### 🔗 관련 링크

|          구분          |                                                                                       링크                                                                                        |
| :--------------------: | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: |
|   🌐 **배포 사이트**   |                                                                          [mopl.site](https://mopl.site/)                                                                          |
|   📖 **이용 가이드**   |                                           [Notion 이용 가이드](https://www.notion.so/2caabb8e46aa801b8a32d0d5bf720ef6?source=copy_link)                                           |
|   🎨 **포트폴리오**    | [Canva 포트폴리오](https://www.canva.com/design/DAG4SpC2BEE/cT2MvW3Pjw7MOGCn3yvaxA/edit?utm_content=DAG4SpC2BEE&utm_campaign=designshare&utm_medium=link2&utm_source=sharebutton) |
|     📋 **팀 노션**     |                                            [팀 노션 페이지](https://www.notion.so/2-2a7abb8e46aa80cca898f383e61fd72a?source=copy_link)                                            |
| 📝 **요구사항 정의서** |                               [Google Sheets](https://docs.google.com/spreadsheets/d/1Tc-qkTBSyD8mJEasUHanuor_W_N4QPC8wnyZyZwcfec/edit?usp=sharing)                               |

# 팀원

|                                                            오명재                                                            |                                                           김민수                                                            |                                                         문은서                                                          |                                                          김이준                                                          |                                                             이지현                                                             |                                                            권용진                                                             |
| :--------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------------------------------------: |:---------------------------------------------------------------------------------------------------------------------------:| :---------------------------------------------------------------------------------------------------------------------------: |
| <a href="https://github.com/Oh-Myeongjae"><img src="https://avatars.githubusercontent.com/u/74406343?v=4" width="100px"></a> | <a href="https://github.com/NanHangBok"><img src="https://avatars.githubusercontent.com/u/105554085?v=4" width="100px"></a> | <a href="https://github.com/kosy00"><img src="https://avatars.githubusercontent.com/u/191211966?v=4" width="100px"></a> | <a href="https://github.com/lkim0402"><img src="https://avatars.githubusercontent.com/u/93887188?v=4" width="100px"></a> | <a href="https://github.com/devlee1011"><img src="https://avatars.githubusercontent.com/u/138750938?v=4" width="100px"></a> | <a href="https://github.com/chaoskyj1120"><img src="https://avatars.githubusercontent.com/u/211930549?v=4" width="100px"></a> |
|                                           컨텐츠 수집 및 관리 <br/>Batch 처리 적용                                           |                                                   사용자 관리 - 시큐리티                                                    |                                             DM 관리 <br/>플레이리스트 관리                                              |                                     실시간 같이보기<br/> 채팅 메시지<br/>OpenSearch                                      |                                                    프로필 관리 <br/>CI/CD 구축                                                     |                                                    알림 관리<br/>리뷰 관리                                                    |
|                    [개인 회고록](https://www.notion.so/2a9abb8e46aa803fa908c1b1f21a783d?source=copy_link)                    |                   [개인 회고록](https://www.notion.so/2a9abb8e46aa803d9af2fd0a875039ca?source=copy_link)                    |                 [개인 회고록](https://www.notion.so/2a9abb8e46aa80f3a60bf05b871befd7?source=copy_link)                  |                  [개인 회고록](https://www.notion.so/2a9abb8e46aa80438c99ea389051a741?source=copy_link)                  |                      [개인 회고록](https://www.notion.so/2a9abb8e46aa80ef9f24f4fb3d24d151?source=copy_link)                      |                    [개인 회고록](https://www.notion.so/2a9abb8e46aa80ebb939f4111a65ae37?source=copy_link)                     |

# 기술 스택

## 🛠 Tech Stack

| Category               | Stacks                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| :--------------------- |:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Backend**            | ![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/spring_boot-%236DB33F.svg?style=flat-square&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white) ![QueryDSL](https://img.shields.io/badge/QueryDSL-007396?style=flat-square&logo=java&logoColor=white) ![Spring Batch](https://img.shields.io/badge/Spring%20Batch-6DB33F?style=flat-square&logo=spring&logoColor=white)                                                                                                                                                                                                                                     |
| **Messaging**          | ![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?style=flat-square&logo=apachekafka&logoColor=white) ![Confluent](https://img.shields.io/badge/Confluent-E2231A?style=flat-square&logo=confluent&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **Database & Storage** | ![PostgreSQL](https://img.shields.io/badge/postgres-%23316192.svg?style=flat-square&logo=postgresql&logoColor=white) ![Amazon RDS](https://img.shields.io/badge/Amazon%20RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white) ![H2](https://img.shields.io/badge/H2%20Database-003B57?style=flat-square&logo=h2&logoColor=white) ![Amazon S3](https://img.shields.io/badge/Amazon%20S3-569A31?style=flat-square&logo=amazons3&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                       |
| **Infra & Deployment** | ![AWS](https://img.shields.io/badge/AWS-%23232F3E.svg?style=flat-square&logo=amazon-aws&logoColor=white) ![Amazon ECS](https://img.shields.io/badge/Amazon%20ECS-FF9900?style=flat-square&logo=amazonecs&logoColor=white) ![Amazon ECR](https://img.shields.io/badge/Amazon%20ECR-FF9900?style=flat-square&logo=amazon-aws&logoColor=white) ![AWS Lambda](https://img.shields.io/badge/AWS%20Lambda-FF9900?style=flat-square&logo=awslambda&logoColor=white) ![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=flat-square&logo=docker&logoColor=white) ![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=flat-square&logo=githubactions&logoColor=white) ![Cloudflare](https://img.shields.io/badge/Cloudflare-F38020?style=flat-square&logo=Cloudflare&logoColor=white) ![Amazon EventBridge](https://img.shields.io/badge/Amazon%20EventBridge-FF4F8B?style=flat-square&logo=amazoneventbridge&logoColor=white) |
| **Testing**            | ![Mockito](https://img.shields.io/badge/Mockito-788BD2?style=flat-square&logo=java&logoColor=white) ![Jacoco](https://img.shields.io/badge/Jacoco-EA2D2E?style=flat-square&logo=jacoco&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| **Auth & API**         | ![JWT](https://img.shields.io/badge/JWT-black?style=flat-square&logo=JSON%20web%20tokens) ![OAuth2](https://img.shields.io/badge/OAuth2-3D3D3D?style=flat-square&logo=oauth&logoColor=white) ![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=flat-square&logo=swagger&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| **Tools**              | ![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=flat-square&logo=github&logoColor=white) ![Discord](https://img.shields.io/badge/Discord-%235865F2.svg?style=flat-square&logo=discord&logoColor=white) ![Notion](https://img.shields.io/badge/Notion-%23000000.svg?style=flat-square&logo=notion&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |

# 시스템 아키텍쳐

(추가 예정)

# 주요 기능

## 콘텐츠 관리

- **콘텐츠 수집**: TMDB, SportsDB API를 통한 영화, 드라마, 스포츠 콘텐츠 자동 수집
- **콘텐츠 검색**: OpenSearch를 활용한 고성능 풀텍스트 검색
- **콘텐츠 평점**: 사용자별 평점 및 리뷰 작성 기능

## 플레이리스트

- **플레이리스트 생성**: 자신만의 기준으로 콘텐츠를 모아 플레이리스트 생성
- **플레이리스트 공유**: 다른 사용자와 플레이리스트 공유
- **플레이리스트 구독**: 관심 있는 플레이리스트 구독 및 알림 수신

## 소셜 기능

- **팔로우/언팔로우**: 다른 사용자를 팔로우하여 활동 추적
- **DM (Direct Message)**: 실시간 1:1 메시지 기능
- **알림 시스템**: SSE(Server-Sent Events)를 통한 실시간 알림

## 실시간 기능

- **같이 보기**: WebSocket을 활용한 실시간 같이 보기 세션 생성 및 참여
- **실시간 채팅**: 같이 보기 세션 내 실시간 채팅 메시지 교환
- **실시간 동기화**: 재생 상태, 시간 등 실시간 동기화

## 사용자 관리

- **회원가입/로그인**: 이메일 기반 회원가입 및 로그인
- **OAuth2 인증**: Google, Kakao 소셜 로그인 지원
- **JWT 인증**: Access Token 및 Refresh Token 기반 인증
- **프로필 관리**: 프로필 이미지 업로드 및 수정 (S3 저장)

## 관리자 기능

- **콘텐츠 관리**: 관리자 콘텐츠 생성, 수정, 삭제
- **사용자 관리**: 사용자 권한 변경, 계정 잠금 등 관리 기능

## 주요 API 엔드포인트

- **인증**: `/api/auth/*` - 로그인, 회원가입, 토큰 갱신 등
- **사용자**: `/api/users/*` - 사용자 정보 조회, 수정 등
- **콘텐츠**: `/api/contents/*` - 콘텐츠 조회, 검색 등
- **플레이리스트**: `/api/playlists/*` - 플레이리스트 생성, 조회, 구독 등
- **리뷰**: `/api/reviews/*` - 리뷰 작성, 조회, 수정 등
- **DM**: `/api/conversations/*`, `/api/direct-messages/*` - 메시지 관련
- **같이 보기**: `/api/watching-sessions/*` - 같이 보기 세션 관리
- **알림**: `/api/notifications/*` - 알림 조회
- **팔로우**: `/api/follows/*` - 팔로우/언팔로우
