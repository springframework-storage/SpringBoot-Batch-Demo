# SpringBoot-Batch

## Spring Batch 의 메타 테이블
### Batch_Job_Instance
* Job Parameter 에 따라 생성되는 테이블
* Spring Batch 가 실행될 때, 외부에서 받을 수 있는 파라미터
* 동일한 Job Parameter 는 여러 개 존재할 수 없습니다.

### Batch_Job_Execution
* Job_Execution 과 Job_Instance 는 ```자식 - 부모``` 관계
* Job_Execution 은 자신의 부모 Job_Instance 가 성공/실패했던 모든 내역을 갖습니다.
* 동일한 Job Parameter 로 성공한 기록이 있을 때만 재수행을 하지 않습니다.

### Batch_Job_Execution_Params
* Batch_Job_Execution 테이블이 생성될 당시 입력 받은 Job Parameter 가 저장됩니다.

---

## 조건별 흐름 제어 (Flow)
### Batch Status vs. Exit Status
* BatchStatus: Job 또는 Step 의 실행 결과를 Spring 에서 기록할 때 사용하는 ```Enum```
    * BatchStatus 로 사용되는 값: ```COMPLETED, STARTING, STARTED, STOPPED, FAILED, ABANDONED, UNKNOWN```
* ExitStatus: Step 의 실행 후 상태
    * ```.on("FAILED").to(stepB())``` 코드에서 ```on``` 메소드가 참조하는 것은 BatchStatus 가 아니고,
     실제 참조되는 값은 Step 의 ExitStatus 입니다.
    * ExitStatus 는 Enum 이 아닙니다.
    * Spring Batch 는 기본적으로 ExitStatus 의 exitCode 는 Step 의 BatchStatus 와 같도록 설정되어 있습니다.
    
### JobExecutionDecider
* Step 들의 Flow 속에서 분기만 담당하는 타입

