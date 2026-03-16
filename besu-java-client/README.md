# Besu ERC-20 Deployment & Multi-node Network

이 프로젝트는 **Hyperledger Besu 다중 노드(Multi-node)** 네트워크를 구성하고, Java와 **Web3j** 라이브러리를 사용하여 해당 네트워크에 연결해 스마트 컨트랙트(ERC-20)를 배포 및 검증하는 전체 과정을 다루는 레포지토리 저장소입니다.

## 🏗️ 하이퍼레저 베수(Hyperledger Besu) 아키텍처 및 코어 설정

[Hyperledger Besu](https://hyperledger.org/projects/besu)는 엔터프라이즈 친화적인 이더리움 클라이언트로, 이더리움 퍼블릭 네트워크는 물론 프라이빗 네트워크(컨소시엄 블록체인) 구축에 최적화되어 있습니다. 

우리의 프라이빗 블록체인 인프라는 성능과 보안을 최적화하기 위해 다음과 같은 고도화된 기술적 구조를 가집니다.

### 1. 제네시스(Genesis) 블록과 QBFT 합의 알고리즘
`genesis.json` 파일을 통해 이더리움 네트워크의 근간이 되는 최초 블록과 네트워크 규칙(Network ID: `2026`)을 정의했습니다. 
특히 무거운 PoW(작업증명) 방식 대신, 엔터프라이즈 환경에 특화된 허가형(Permissioned) 합의 알고리즘인 **QBFT(Quorum Byzantine Fault Tolerance)**를 채택했습니다. 
- **빠른 트랜잭션 처리 (Block Period)**: 블록 생성 주기(`blockPeriodSeconds`)를 **2초** 단위로 고정하여 트랜잭션이 즉각적으로 컨펌되도록 설계했습니다.
- **에포크(Epoch) 동기화**: `epochLength`를 30,000으로 설정해 주기적으로 검증자(Validator) 풀을 갱신하고 검증합니다.
- **Gas-Free (가스비 무료)**: 원활한 컨트랙트 테스트와 사내 토큰 배포를 위해 블록 `gasLimit`을 최대로 개방하고(`0x1fffffffffffff`), 기본 송금 가스 비용(`zeroBaseFee: true`)을 없애 수수료 부담 없이 구축되었습니다.

### 2. 멀티 노드 인프라와 RPC 구조 (Docker Compose)
네트워크 인프라는 서버 리소스 낭비를 막기 위해 Docker Compose 단위로 관리되며, 역할을 분리한 2개의 코어 노드를 띄우는 구조입니다.

- **Validator 노드 (Node 1)**: 합의 과정에 직접 참여하여 실질적으로 블록을 생성하고 체인을 이어가는 검증 노드입니다. (포트 `8545` 개방)
- **RPC 전용 노드 (Node 2)**: 외부 클라이언트(Web3 API)의 트래픽을 처리하기 전용으로 세팅된 노드로, Validator의 `bootnodes` 주소를 바라보고 동기화를 수행하며 사용자 트랜잭션을 수집합니다. 외부와 `8547` 포트로 통신합니다.

**RPC 통신 설정 (`--rpc-http-API`)**:
각 노드는 Besu 커맨드라인 옵션을 통해 HTTP JSON-RPC 인터페이스를 엽니다. 자바 클라이언트가 컨트랙트를 배포할 수 있도록 API 엔드포인트를 열어두었습니다.
- 허용 API 목록: `ETH`, `NET`, `QBFT`, `WEB3`, `TXPOOL`
- 크로스 도메인(Cors) 제약 해제: `--rpc-http-cors-origins=*` 및 `--host-allowlist=*`

---

## 📁 전체 프로젝트 구조 (Repository Structure)

```text
hyperledger-web3/
├── besu-network/            # 하이퍼레저 베수 프라이빗 네트워크 구성 (Infra)
│   ├── docker-compose.yml   # Node1(Validator), Node2(RPC) 구조를 정의한 도커 컨테이너 설정
│   ├── genesis.json         # 네트워크 ID 2026, QBFT 알고리즘이 명시된 Genesis 환경 설정
│   ├── node1/               # 첫 번째 데이터 노드의 키(Key) 및 통신 데이터 디렉토리
│   └── node2/               # 두 번째 데이터 노드의 키(Key) 및 통신 데이터 디렉토리
│
└── besu-java-client/        # 프라이빗 네트워크와 상호작용하는 Java 애플리케이션
    ├── pom.xml              # Maven 설정 및 Web3j(v4.9.4) 의존성
    └── src/main/java/com/example/BesuClient.java  # 노드 RPC 연결 테스트 및 통신 클래스
```

## 🚀 개발 및 연결 과정 (Development Process)

우리는 Besu 환경 구축부터 Java 애플리케이션 연동까지 아래의 순서로 작업을 진행해왔습니다.

### 1. Besu 멀티 노드 네트워크 구축 (`besu-network`)
- 원격 서버(`192.168.160.146`)에서 **Docker Compose**를 사용하여 두 개의 Besu 노드(Node1, Node2)를 실행하는 프라이빗 블록체인 환경을 구성했습니다.

### 2. Java 클라이언트 환경 구성 (`besu-java-client`)
- `pom.xml`에 이더리움 및 호환 네트워크와 상호작용하기 위해 `org.web3j:core` 의존성을 추가했습니다.

### 3. Besu 노드 RPC 연결 테스트 (`BesuClient.java`)
스마트 컨트랙트를 사전 배포하기 위해, Java 측에서 구동 중인 Besu 노드(`http://192.168.160.146:8545`)의 HTTP JSON-RPC 엔드포인트에 요청을 보내 서버 통신을 테스트했습니다.
- **`Web3j.build(new HttpService(nodeUrl))`**: HTTP 엔드포인트를 통해 통신 객체 생성.
- 앞서 선언한 RPC API 중 `WEB3`, `ETH` 도메인을 호출해 클라이언트 버전을 검증하고, 정상적으로 최신 터미널 블록넘버가 조회되는지 증명합니다.

### 4. 향후 목표 (ERC-20 배포)
- Solidity(`.sol`)로 작성된 ERC-20 컨트랙트를 컴파일.
- Web3j CLI를 통해 컴파일된 ABI 객체를 바탕으로 Java Wrapper 클래스 변환.
- 생성된 Wrapper 클래스를 통해 노드에 실제 ERC-20 컨트랙트를 트랜잭션으로 배포 및 상호작용.

## 💻 클라이언트 실행 방법 (How to Run Client)

프로젝트 내부의 `besu-java-client` 경로에서 아래 Maven 명령어를 실행하여 노드 연결을 테스트할 수 있습니다.

```bash
cd besu-java-client
mvn clean compile
mvn exec:java
```
