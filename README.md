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
    
---

## Chunk 지향 처리
Spring Batch 의 큰 장점 중 하나로 Chunk 지향 처리가 있습니다.

### Chunk
* Spring Batch 에서의 Chunk 란 데이터 덩어리로, 작업할 때 각 커밋 사이에 처리되는 row 수를 말합니다.
* 즉, Chunk 지향 처리란 한 번에 하나씩 데이터를 읽어 Chunk 라는 덩어리를 만들고, Chunk 단위로 트랜잭션을 다루는 것을 의미합니다.
* Chunk 단위로 트랜잭션을 수행하기 때문에 실패할 경우 해당 Chunk 만큼만 롤백되고, 이전에 커밋된 트랜잭션 범위까지는 반영됩니다.
* **Reader 와 Processor 에서는 1건씩 다뤄지고, Writer 에선 Chunk 단위로 처리됩니다.**

### ChunkOrientedTasklet
* Chunk 지향 처리의 전체 로직을 다루는 것은 ```ChunkOrientedTasklet``` 클래스입니다.
* ```ChunkOrientedTasklet``` 의 ```execute()``` 메소드 내부 코드
    * ```chunkProvider.provide()``` 로 Reader 에서 Chunk Size 만큼 데이터를 가져옵니다.
    * ```chunkProcessor.process()``` 에서 Reader 로 받은 데이터를 가공(Processor)하고 저장(Writer)합니다.
* ```chunkProvider.provide()``` 내부
    * ```inputs``` 가 Chunk Size 만큼 쌓일 때까지 ```read()``` 를 호출합니다.
    * 실제로는 ```ItemReader.read``` 를 호출합니다.
    * **즉, ```ItemReader.read``` 에서 1건씩 데이터를 조회해 Chunk Size 만큼 데이터를 쌓는 것이 ```provide()``` 가 하는 일입니다.**

### SimpleChunkProcessor
* Processor 와 Writer 로직을 담고 있는 것은 ```ChunkProcessor``` 가 담당합니다.
* ```ChunkProcessor``` 는 인터페이스이기 때문에 실제 구현체가 있어야 합니다.
    * 기본적으로 사용되는 것이 ```SimpleChunkProcessor``` 입니다.
* 처리를 담당하는 핵심 로직은 ```process()```입니다.
    * ```Chunk<I> inputs``` 를 파라미터로 받습니다.
        * 이 데이터는 앞서 ```chunkProvider.provide()``` 에서 받은 Chunk Size 만큼 쌓인 item 입니다.
    * ```transform()``` 에서는 전달 받은 ```inputs``` 을 ```doProcess()``` 로 전달하고 변환값을 받습니다.
    * ```transform()``` 을 통해 가공된 대량의 데이터는 ```write()``` 를 통해 일괄 저장됩니다.
        * ```write()``` 는 저장이 될 수도 있고, 외부 API 로 전송할 수도 있습니다.
        * 이는 개발자가 ItemWriter 를 어떻게 구현했는지에 따라 달라집니다.
    * 여기서 ```transform()``` 은 반복문을 통해 ```doProcess()``` 를 호출합니다.
        * 해당 메소드는 ItemProcessor 의 ```process()``` 를 사용합니다.
        * ```doProcess()``` 를 처리하는데 만약 ItemProcessor 가 없다면, 즉 item 을 그대로 반환하고 있다면
         ItemProcessor 의 ```process()``` 로 가공하여 반환합니다.
* **이렇게 가공된 데이터들은 SimpleChunkProcessor 의 ```doWrite()``` 를 호출하여 일괄 처리합니다.**

### Page Size vs Chunk Size
기존의 Spring Batch 에서 많아 사용하던 PagingItemReader 가 있습니다. 
* Chunk Size 는 한번에 처리될 트랜잭션 단위
* Page Size 는 한번에 조회할 Item 의 양

PagingItemReader 의 부모 클래스인 ```AbstractItemCountingItemStreamItemReader``` 의 ```read()``` 메소드 내부
* 읽어올 데이터가 있다면 ```doRead()``` 를 호출합니다.
    * ```doRead()``` 에서는 현재 읽어올 데이터가 없거나, Page Size 를 초과한 경우 ```doReadPage()``` 를 호출합니다.
        * 읽어올 데이터가 없는 경우는 read 가 처음 시작할 때는 말합니다.
        * Page Size 를 초과하는 경우는 Page Size 가 10인데, 읽어야 하는 데이터가 11번째 데이터인 경우입니다.
        * **즉, Page 단위로 끊어서 조회하는 것입니다.**
    * ```doReadPage()``` 부터는 하위 구현 클래스에서 각자만의 방식으로 페이징 쿼리를 생성합니다.
        * 보편적으로 많이 사용하는 것은 ```JpaPagingItemReader``` 입니다. 
        
JpaPagingItemReader 의 ```doReadPage()``` 의 코드
* Reader 에서 지정한 Page Size 만큼 ```offSet, limit``` 값을 지정하여 페이징 쿼리를 생성(```createQuery()```)하고,
 사용 (```query.getResultList()```)합니다.
* 쿼리 실행 결과는 ```results``` 에 저장합니다.
* **이렇게 저장된 ```results``` 에서 ```read()``` 가 호출될 때마다 하나씩 꺼내어 전달합니다.**

### 만약 Page Size 가 10, Chunk Size 가 50 이라면 
* ```ItemReader``` 에서 Page 조회가 5번 일어났을 때, 1번의 트랜잭션이 발생하여 Chunk 가 처리됩니다.
* 한 번의 트랜잭션 처리를 위해 5번의 쿼리 조회가 발생하기 때문에 성능상 이슈가 발생할 수 있습니다.
* 그래서 Spring Batch 의 PagingItemReader 에는 ```상당히 큰 페이지 크기를 설정하고 페이지 크기와 일치하는 커밋 간격을 사용하면 성능이 향상됩니다.```
 라는 주석이 작성되어 있습니다.
* **2개 값을 일치시키는 것이 보편적으로 좋은 방법이므로 일치시키는 것을 추천합니다.**