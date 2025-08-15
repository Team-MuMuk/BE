# BE

## Contributors
| <img src="https://avatars.githubusercontent.com/u/165489156?v=4" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/80953694?v=4" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/95339052?v=4" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/201078502?v=4" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/186348397?v=4" width="100" height="100"> |
|:---:|:---:|:---:|:---:|:---:|
| [박민음(Lead)](https://github.com/parkmineum) | [유완규](https://github.com/beans3142) | [한재민](https://github.com/jaemin0413) | [박종찬](https://github.com/Jongchanpark22) | [이가은](https://github.com/GaEun132) |

<br> 


## Tech Stack
| **구분** | **기술** |
| --- | --- |
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.3.1 |
| **Database** | PostgreSQL, Redis |
| **CI / CD** | GitHub Actions, Dockerhub |
| **Monitoring** | Grafana, Prometeus, Promtail, Loki |
| Test | K6, JUnit |


<br> 

## System Architecture
<img width="2546" height="1044" alt="스크린샷 2025-08-15 오후 4 12 40" src="https://github.com/user-attachments/assets/7a326e07-93b6-444f-8b42-ac8b09c4e18f" />
- 팀원 모두가 운영 환경상의 리소스와 로그 추적이 가능하도록 별도의 도메인으로 분리하여 모니터링 서버를 구축하였습니다.  <br>
- 별도의 인증 과정을 통해 Grafana 서버에 접속하며, 어플리케이션(WAS) 와 Nginx, RDS 등의 상황을 실시간으로 알 수 있게 하여 개발 환경을 최적화하였습니다.  <br>
- 부하 테스트를 통해 서비스의 병목 지점과 DB 성능 최적화를 진행하였습니다. 


<br> 

### Branch
`컨벤션명/#이슈번호`

### Commit Convention
| 커밋 타입 | 설명 | **커밋 메시지 예시** |
| --- | --- | --- |
| ✨ **Feat** | 새로운 기능 추가 | `[FEAT] #이슈번호: 기능 추가` |
| 🐛 **Fix** | 버그 수정 | `[FIX] #이슈번호: 오류 수정` |
| 📄 **Docs** | 문서 수정 | `[DOCS] #이슈번호: README 파일 수정` |
| ♻️ **Refactor** | 코드 리팩토링 | `[REFACTOR] #이슈번호: 함수 구조 개선` |
| 📦 **Chore** | 빌드 업무 수정, 패키지 매니저 수정 등 production code와 무관한 변경 | `[CHORE] #이슈번호: .gitignore 파일 수정` |
| 💬 **Comment** | 주석 추가 및 변경 | `[COMMENT] #이슈번호: 함수 설명 주석 추가` |
| 🔥 **Remove** | 파일 또는 폴더 삭제 | `[REMOVE] #이슈번호: 불필요한 파일 삭제` |
| 🚚 **Rename** | 파일 또는 폴더명 수정 | `[RENAME] #이슈번호: 폴더명 변경` |


### Issue Template
```
## 어떤 기능인가요?

> 추가하려는 기능에 대해 간결하게 설명해주세요

## 작업 상세 내용

- [ ] TODO
- [ ] TODO
- [ ] TODO

## 참고할만한 자료(선택)
```


### Pull Request Template
```
## 🎋 이슈 및 작업중인 브랜치

-

## 🔑 주요 내용

-


## Check List

- [ ] **Reviewers** 등록을 하였나요?
- [ ] **Assignees** 등록을 하였나요?
- [ ] **라벨(Label)** 등록을 하였나요?
- [ ] PR 머지하기 전 반드시 **CI가 정상적으로 작동하는지 확인**해주세요!
```

