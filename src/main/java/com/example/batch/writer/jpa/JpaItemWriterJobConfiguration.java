package com.example.batch.writer.jpa;

import com.example.batch.domain.Pay;
import com.example.batch.domain.Pay2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaItemWriterJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final EntityManagerFactory entityManagerFactory;

  private static final int chunkSize = 10;

  @Bean
  public Job jpaItemWriterJob() {
    return jobBuilderFactory.get("jpaItemWriterJob")
            .start(jpaItemWriterStep())
            .build();
  }

  @Bean
  public Step jpaItemWriterStep() {
    return stepBuilderFactory.get("jpaItemWriterStep")
            .<Pay, Pay2>chunk(chunkSize)
            .reader(jpaItemWriterReader())
            .processor(jpaItemProcessor())
            .writer(jpaItemWriter())
            .build();
  }

  @Bean
  public JpaPagingItemReader<Pay> jpaItemWriterReader() {
    return new JpaPagingItemReaderBuilder<Pay>()
            .name("jpaItemWriterReader")
            .entityManagerFactory(entityManagerFactory)
            .pageSize(chunkSize)
            .queryString("SELECT p FROM Pay p")
            .build();
  }

  /**
   * JdbcBatchItemWriter 와 다르게 Processor 가 추가되었습니다.
   * 이유는 Pay Entity 를 읽어서 Writer 에는 Pay2 Entity 를 전달해주기 위함입니다.
   * - Reader 에서 읽은 데이터를 가공해야할 때 Processor 가 필요합니다.
   *
   * @return
   */
  @Bean
  public ItemProcessor<Pay, Pay2> jpaItemProcessor() {
    return pay -> new Pay2(pay.getAmount(), pay.getTxName(), pay.getTxDateTime());
  }

  /**
   * JpaItemWriter 는 JPA 를 사용하기 때문에 영속성 관리를 위해 EntityManager 를 할당해줘야 합니다.
   * - 일반적으로 'spring-boot-starter-data-jpa' 를 의존성에 등록하면 EntityManager 가 Bean 으로 자동 생성되어
   * DI 코드만 추가하면 됩니다.
   *
   * JdbcBatchItemWriter 에 비해 필수값이 EntityManager 뿐이라 체크할 요소가 적다는 것이 장점 아닌 장점입니다.
   *
   * JpaItemWriter 는 JdbcBatchItemWriter 와 달리 넘어온 Entity 를 DB 에 반영합니다.
   * 즉, Entity 클래스를 제네릭 타입으로 받아야만 합니다.
   * - JdbcBatchItemWriter 의 경우 DTO 클래스를 받더라도 sql 로 지정된 쿼리가 실행되니 문제가 없지만,
   * JpaItemWriter 는 넘어온 Item 을 그래도 entityManager.merge() 로 테이블에 반영하기 때문입니다.
   *
   * @return
   */
  @Bean
  public JpaItemWriter<Pay2> jpaItemWriter() {
    JpaItemWriter<Pay2> jpaItemWriter = new JpaItemWriter<>();
    jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
    return jpaItemWriter;
  }

}
