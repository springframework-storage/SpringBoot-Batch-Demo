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
기존의 Spring Batch 에서 많이 사용하던 PagingItemReader 가 있습니다. 
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

---

## ItemReader
* 위 과정을 통해 Spring Batch 가 Chunk 지향 처리를 하고 있으며, 이는 Job 과 Step 으로 구성되어 있음을 알았습니다.
* Step 은 Tasklet 단위로 처리되고, Tasklet 중에서 ```ChunkOrientedTasklet``` 을 통해 Chunk 를 처리하며 이를 구성하는
 3 요소로 ```ItemReader, ItemWriter, ItemProcessor``` 가 있음을 알았습니다.
    > 즉, ItemReader, ItemWriter, ItemProcessor 의 묶음 역시 Tasklet 이란 얘기입니다.<br>
    이들의 묶음을 ChunkOrientedTasklet 에서 관리하기 때문입니다.
    
### ItemReader 소개
* ItemReader 는 데이터를 읽습니다. 꼭 DB 의 데이터만을 이야기하는 것은 아닙니다.
* Spring Batch 에서 지원하지 않는 Reader 가 필요할 경우 직접 해당 Reader 를 만들 수도 있습니다.
* Spring Batch 의 Reader 에서 읽어올 수 있는 데이터 유형은 다음과 같습니다.
    * 입력 데이터에서 읽어오기
    * 파일에서 읽어오기
    * DB 에서 읽어오기
    * JMS 등 다른 소스에서 읽어오기
    * 본인만의 커스텀한 Reader 로 읽어오기

* ItemReader 의 가장 대표적인 구현체는 ```JdbcPagingItemReader``` 입니다
    * ItemReader 외에 **ItemStream 인터페이스도 같이 구현하고 있습니다.**
    * ItemReader 는 ```read()``` 라는 데이터를 읽어오는 메소드만 갖고 있습니다.
    * **ItemStream 인터페이스는 주기적으로 상태를 저장하고 오류가 발생하면 해당 상태에서 복원하기 위한 Marker 인터페이스입니다.
     즉, 배치 프로세스의 실행 컨텍스트와 연계해서 ItemReader 의 상태를 저장하고 실패한 곳에서 다시 실행할 수 있게 해주는 역할을 합니다.
        * ItemStream 의 3개 메소드는 다음과 같은 역할을 합니다.
            > open(), close() 는 스트림을 열고 닫습니다.<br>
            update() 를 사용하면 Batch 처리의 상태를 업데이트 할 수 있습니다.
    * **개발자는 ItemReader 와 ItemStream 인터페이스를 직접 구현하여 원하는 형태의 ItemReader 를 만들 수 있습니다.**
    
### Database Reader
Spring Framework 의 강점 중 하나는 개발자가 비즈니스 로직에만 집중할 수 있도록 JDBC 와 같은 문제점을 추상화한 것입니다.
> 이를 서비스 추상화라고 합니다.<br>
그래서 Spring Batch 는 Spring Framework 의 JDBC 기능을 확장했습니다.

* 일반적으로 배치 작업은 많은 양의 데이터를 처리해야 합니다.
* 그러나 JdbcTemplate 는 분할 처리를 지원하지 않기 때문에 (쿼리 결과 그대로 반환) 개발자가 직접 ```limit, offset``` 을 
 사용하는 등의 작업이 필요합니다.
* Spring Batch 는 이런 문제점을 해결하기 위해 2개의 Reader 타입을 지원합니다.
* Cursor 는 실제로 JDBC ResultSet 의 기본 기능입니다.
    * ResultSet 이 open 될 때마다 ```next()``` 메소드가 호출되어 DB 의 데이터가 반환됩니다.
    * 이를 통해 필요에 따라 DB 에서 데이터를 Streaming 할 수 있습니다.
    * Cursor 방식은 DB 와 커넥션을 맺은 후, Cursor 를 한칸씩 옮기면서 지속적으로 데이터를 가져옵니다.
    * Cursor 기반 ItemReader 구현체
        * JdbcCursorItemReader
        * HibernateCursorItemReader
        * StoredProcedureItemReader
* 반면 Paging 은 좀 더 많은 작업을 필요로 합니다.
    * Paging 개념은 페이지라는 Chunk 로 DB 에서 데이터를 검색한다는 것입니다.
    * **즉, 페이지 단위로 한번에 데이터를 조회하는 방식입니다.**
    * Paging 방식은 개발자가 지정한 Page Size 만큼 데이터를 가져옵니다.
    * Paging 기반 ItemReader 구현체
        * JdbcPagingItemReader
        * HibernatePagingItemReader
        * JpaPagingItemReader
        
### CursorItemReader (JdbcCursorItemReader)
JPA 에는 CursorItemReader 가 없습니다.
* [JdbcCursorItemReaderJobConfiguration](./src/main/java/com/example/batch/reader/jdbc/JdbcCursorItemReaderJobConfiguration.java)

**CursorItemReader 주의사항**
* DB 와 SocketTimeout 을 충분히 큰 값으로 설정해야만 합니다.
* Cursor 는 하나의 Connection 으로 Batch 가 끝날 때까지 사용되기 때문에 Batch 가 끝나기 전에 DB 와 어플리케이션의
 Connection 이 먼저 끊어질 수 있습니다.
* 그래서 Batch 수행 시간이 오래 걸리는 경우에는 PageItemReader 를 사용하는게 좋습니다.
* Paging 의 경우 한 페이지를 읽을 때마다 Connection 을 맺고 끊기 때문에 아무리 많은 데이터라도
 타임아웃과 부하 없이 수행될 수 있습니다. 

### PagingItemReader (JdbcPagingItemReader)
* DB Cursor 를 사용하는 대신 여러 쿼리를 실행하여 각 쿼리가 결과의 일부를 가져오는 방법도 있습니다.
    * 이런 처리 방법을 Paging 이라고 합니다.
* Spring Batch 에서는 ```offset, limit``` 을 PageSize 에 맞게 자동으로 생성합니다.
    * 다만 각 쿼리는 개별적으로 실행한다는 것을 유의해야 합니다.
    * 각 페이지마다 새로운 쿼리를 실행하므로 페이징 시 결과를 **정렬**하는 것이 중요합니다.
    * 데이터 결과의 순서가 보장될 수 있도록 ```order by``` 가 권장됩니다.
    
* [JdbcPagingItemReaderJobConfiguration](./src/main/java/com/example/batch/reader/jdbc/JdbcPagingItemReaderJobConfiguration.java)
* 쿼리 로그를 보면 ```LIMIT 10``` 이 들어간 것을 알 수 있습니다. (작성한 코드에는 Limit 선언이 없음)
* JdbcPagingItemReader 에서 선언된 fetchSize 에 맞게 자동으로 쿼리에 추가해줬기 때문입니다.
* 만약 조회할 데이터가 10개 이상이라면 ```offset``` 으로 적절하게 다음 fetchSize 만큼을 가져올 수 있습니다.

### JpaPagingItemReader
JPA 는 Hibernate 와 많은 유사점을 갖고 있지만, 한 가지 다른 것이 있다면 Hibernate 에선 Cursor 가 지원되지만
 JPA 에서 Cursor 기반 DB 접근을 지원하지 않습니다.
* [JpaPagingItemReaderJobConfiguration](./src/main/java/com/example/batch/reader/jpa/JpaPagingItemReaderJobConfiguration.java)

**PagingItemReader 주의사항**
* 정렬 ```order``` 가 무조건 포함되어야 합니다.
* 많은 데이터를 Chunk 로 분할 조회한다면, 만약 데이터 개수가 4만건이고 Chunk Size 가 1만이라고 가정해봅니다.
    * 4번의 쿼리가 limit 의 시작 포인트만 변경된 채 수행됩니다.
    * 각각 별도로 수행되는 쿼리이기 때문에 정렬 기준이 정해져 있지 않다면 쿼리마다 각자의 정렬 기준을 만들어
     실행하게 됩니다.
    * 이것으로 인해 원했던 결과를 얻지 못하는 문제가 발생할 수 있습니다.

**해결책**
* Order by
    * 가장 보편적인 방법은 ```order by id``` 와 같이 queryString 에 고유한 정렬 기준을 포함
* CursorItemReader
    * JPA 의 구현체는 없지만, Jdbc, Hibernate, MyBatis 에는 CursorItemReader 라는 Reader 구현체가 존재합니다.
    * ResultSet 과 직접 연동하여 데이터를 읽어오는 것인데, 일종의 stream 과 같다고 보면 됩니다.
    * 사실상 전체를 조회하여 stream 처럼 지속적으로 데이터를 가져오는 방식이기에 페이징 이슈는 발생하지 않습니다.
    * 성능 또한 PagingItemReader 보다 좋습니다.
    * 하지만, 무조건 Cursor 를 사용하지 않는 이유는 다음과 같습니다.
    * 한번에 가져오는 데이터의 양이 많을 경우 Batch 가 뻗을 수 있습니다.
        * Paging 해서 조회하는 것이 아니기 때문에 전체 조회 결과 데이터가 클 경우 문제가 발생할 수 있습니다.
    * Thread Safe 하지 않습니다.
        * Multi Thread 로 Batch 를 구현해야 하는 상황이라면 PagingItemReader 를 사용해야만 합니다.
        
### ItemReader 주의사항
* JpaRepository 를 ListItemReader, QueueItemReader 에 사용하면 안됩니다.
    * ```new ListItemReader<>(jpaRepository.findByAge(age))``` 로 Reader 로 구현할 경우
        * Spring Batch 의 장점인 Paging & Cursor 구현이 없어 대규모 데이터 처리가 불가능합니다.
            > 물론 Chunk 단위 트랜잭션은 가능합니다.
    * 만약 JpaRepository 를 사용해야 한다면, RepositoryItemReader 를 사용하는 것을 추천합니다.
        * Paging 을 기본적으로 지원합니다.
    * Hibernate, JPA 등 영속성 컨텍스트가 필요한 Reader 사용시 fetchSize 와 chunkSize 는 같은 값을 유지해야 합니다.
    
---


