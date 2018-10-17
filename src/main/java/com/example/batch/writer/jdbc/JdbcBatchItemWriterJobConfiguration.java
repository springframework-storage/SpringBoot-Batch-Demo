package com.example.batch.writer.jdbc;

import com.example.batch.domain.Pay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcBatchItemWriterJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final DataSource dataSource;

  private static final int chunkSize = 10;

  @Bean
  public Job jdbcBatchItemWriterJob() {
    return jobBuilderFactory.get("jdbcBatchItemWriterJob")
            .start(jdbcBatchItemWriterStep())
            .build();
  }

  @Bean
  public Step jdbcBatchItemWriterStep() {
    return stepBuilderFactory.get("jdbcBatchItemWriterStep")
            .<Pay, Pay>chunk(chunkSize)
            .reader(jdbcBatchItemWriterReader())
            .writer(jdbcBatchItemWriter())
            .build();
  }

  @Bean
  public JdbcCursorItemReader<Pay> jdbcBatchItemWriterReader() {
    return new JdbcCursorItemReaderBuilder<Pay>()
            .fetchSize(chunkSize)
            .dataSource(dataSource)
            .rowMapper(new BeanPropertyRowMapper<>(Pay.class))
            .sql("SELECT id, amount, tx_name, tx_date_time FROM pay")
            .name("jdbcBatchItemWriter")
            .build();
  }

  /**
   * reader 에서 넘어온 데이터를 하나씩 출력하는 Writer
   *
   * JdbcBatchItemWriterBuilder 는 다음과 같은 설정값을 갖는다.
   *
   * # assertUpdates (parameterType: boolean)
   * - 적어도 하나의 항목이 행을 업데이트하거나 삭제하지 않을 경우, 예외를 throw 할지 여부를 설정
   * - 기본값은 true, Exception: EmptyResultDataAccessException
   *
   * # columnMapped (parameterType: 없음)
   * - Key, Value 기반으로 Insert SQL 의 Values 를 맵핑한다. (ex: Map<String Object>)
   *
   * # beanMapped (parameterType: 없음)
   * - POJO 기반으로 Insert SQL 의 Values 를 맵핑한다.
   *
   * @return
   */
  @Bean // beanMapped() 를 사용할 때는 필수
  public JdbcBatchItemWriter<Pay> jdbcBatchItemWriter() {
    /**
     * JdbcBatchItemWriter 의 설정에서 주의할 점
     * - JdbcBatchItemWriter 의 제네릭 타입은 Reader 에서 넘겨주는 값의 타입이다.
     * - Pay2 테이블에 데이터를 넣은 Writer 이지만, 선언된 제네릭 타입은 Reader/Processor 에서 넘겨준 Pay 클래스이다.
     */
    return new JdbcBatchItemWriterBuilder<Pay>()
            .dataSource(dataSource)
            .sql("INSERT INTO pay2(amount, tx_name, tx_date_time) VALUES (:amount, :txName, :txDateTime)")
            .beanMapped()
            .build();

    /**
     * 위와 아래의 차이 즉, beanMapped() 와 columnMapped() 의 차이는
     * Reader 에서 Writer 로 넘겨주는 타입이 Map<String, Object> 냐, Pay.class 와 같은 POJO 타입이냐 입니다.
     */

//    return new JdbcBatchItemWriterBuilder<Map<String, Object>>()  // Map 사용
//            .columnMapped()
//            .dataSource(dataSource)
//            .sql("INSERT INTO pay2(amount, tx_name, tx_date_time) VALUES (:amount, :txName, :txDateTime)")
//            .build();
  }

}
