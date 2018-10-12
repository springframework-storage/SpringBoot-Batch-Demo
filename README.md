# SpringBoot-Batch

---

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

---

## Scope & Job Parameter
* 여기서 말하는 Scope 란 ```@StepScope, @JobScope``` 입니다.
* Spring Batch 의 경우 외부 또는 내부에서 Parameter 를 받아 여러 Batch 컴포넌트에서 사용할 수 있게 지원합니다. 
이 파라미터를 Job Parameter 라고 합니다.
* Job Parameter 를 사용하기 위해서는 항상 Spring Batch 전용 ```Scope``` 를 선언해야만 합니다.
* 사용법은 다음과 같이 SpEL 로 선언하여 사용하면 됩니다.
    * ```@Value("#{jobParameters[파라미터명]}")```
    * ```jobParameters``` 외에도 ```jobExecutionContext, stepExecutionContext``` 등도 SpEL 로 사용할 수 있습니다.
    * @JobScope 에서는 ```stepExecutionContext``` 는 사용할 수 없고, ```jobParameters, jobExecutionContext``` 만 사용할 수 있습니다.
    
### @StepScope & @JobScope
* Spring Batch 는 ```@StepScope, @JobScope``` 라는 아주 특별한 Bean Scope 를 지원합니다.
    * ( Spring Bean 의 기본 Scope 는 singleton )

* Spring Batch 컴포넌트 (Tasklet, ItemReader, ItemWriter, ItemProcessor 등)에 ```@StepScope``` 를 사용하면
* Spring Batch 가 Spring 컨테이너를 통해 지정된 Step 의 실행 시점에 해당 컴포넌트를 Spring Bean 으로 생성합니다.
* ```@JobScope``` 는 Job 실행 시점에 Bean 이 생성됩니다.
* 즉, Bean 의 생성 시점을 지정된 Scope 가 실행되는 시점으로 지연시킵니다.

이렇게 Bean 의 생성 시점을 어플리케이션 실행 시점이 아닌, Step 혹은 Job 의 실행 시점으로 지연시킴으로써
 얻는 장점은 크게 2가지가 있습니다.
* 첫째, Job Parameter 의 Late Binding 이 가능합니다.
    * Job Parameter 를 StepContext 또는 JobExecutionContext 레벨에서 할당할 수 있습니다.
    * 꼭 Application 이 실행되는 시점이 아니더라도 Controller, Service 와 같은 비즈니스 로직 처리 단계에서
     Job Parameter 를 할당할 수 있습니다.
* 둘째, 동일한 컴포넌트를 병렬 혹은 동시에 사용할 때 유용합니다.
    * Step 안에 Tasklet 이 있고, 이 Tasklet 에는 멤버 변수와 이 멤버 변수를 변경하는 로직이 있다고 가정합니다.
    * 이 경우 ```@StepScope``` 없이 Step 을 병렬로 실행시키면 서로 다른 Step 에서 하나의 Tasklet 을 두고
     마구잡이로 상태를 변경하려고 할 것입니다.
    * 하지만, ```@StepScope``` 가 있다면 각각의 Step 에서 별도의 Tasklet 을 생성하고 관리하기 때문에
     서로의 상태를 침범할 일이 없습니다.
     
### Job Parameter
* Job Parameters 는 ```@Value``` 를 통해서 가능합니다.
* Job Parameters 는 Step 이나, Tasklet, Reader 등 Batch 컴포넌트 Bean 의 생성 시점에 호출할 수 있습니다.
    * 정확히는 ```@StepScope, @JobScope``` Bean 을 생성할 때만 호출할 수 있습니다.