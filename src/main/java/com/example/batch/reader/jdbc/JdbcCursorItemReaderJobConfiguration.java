package com.example.batch.reader.jdbc;

import com.example.batch.domain.Pay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;

/**
 * reader 는 Tasklet 이 아니기 때문에 reader 만으로는 수행될 수 없고, 간단한 출력 Writer 를 하나 추가합니다.
 * Processor 는 필수가 아닙니다.
 * - reader 에서 읽은 데이터에 대해 크기 변경 로직이 없다면 processor 를 제외하고 writer 만 구현하면 됩니다.
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcCursorItemReaderJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final DataSource dataSource;

  private static final int chunkSize = 10;

  @Bean
  public Job jdbcCursorItemReaderJob() {
    return jobBuilderFactory.get("jdbcCursorItemReaderJob")
            .start(jdbcCursorItemReaderStep())
            .build();
  }

  @Bean
  public Step jdbcCursorItemReaderStep() {
    return stepBuilderFactory.get("jdbcCursorItemReaderStep")
            /**
             * 첫 번째 Pay 는 Reader 에서 반환할 타입,
             * 두 번째 Pay 는 Writer 에 파라미터로 넘어올 타입
             * chunkSize 로 인자값을 넣는 것은 Reader & Writer 가 묶을 Chunk 트랜잭션 범위
             */
            .<Pay, Pay>chunk(chunkSize)
            .reader(jdbcCursorItemReader())
            .writer(jdbcCursorItemWriter())
            .build();
  }

  /**
   * 아래 코드를 jdbcTemplate 로 구현하면 다음과 같다
   *
   * JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
   * List<Pay> payList = jdbcTemplate.query("SELECT id, amount, tx_name, tx_date_time FROM pay", new BeanPropertyRowMapper<>(Pay.class));
   *
   * 거의 차이가 없지만, ItemReader 의 가장 큰 장점은 데이터를 Streaming 할 수 있다는 것
   * read() 메소드는 데이터를 하나씩 가져와 ItemWriter 로 데이터를 전달하고, 다음 데이터를 다시 가져온다.
   * 이를 통해 reader & processor & writer 가 Chunk 단위로 수행되고 주기적으로 Commit 된다.
   * 이는 고성능의 배치 처리에서 핵심이다.
   *
   * @return
   */
  @Bean
  public JdbcCursorItemReader<Pay> jdbcCursorItemReader() {
    return new JdbcCursorItemReaderBuilder<Pay>()
            /**
             * fetchSize: DB 에서 한번에 가져올 데이터의 양
             * Paging 과는 다른 것이 Paging 은 실제 쿼리에 limit, offset 을 이용해 분할 처리
             * 반면, Cursor 의 쿼리는 분할 처리 없이 실행되나 내부적으로 가져오는 데이터는 Fetch Size 만큼 가져와
             * read() 를 통해 하나씩 가져온다.
             */
            .fetchSize(chunkSize)
            /**
             * DB 에 접근하기 위해 사용할 DataSource 객체를 할당
             */
            .dataSource(dataSource)
            /**
             * 쿼리 결과를 Java 인스턴스로 맵핑하기 위한 Mapper
             * 커스텀하게 생성해서 사용할 수도 있지만, 그럴 경우 매번 Mapper 클래스를 생성해야 하기 떄문에
             * 보편적으로는 Spring 에서 공식적으로 지원하는 BeanPropertyRowMapper.class 를 많이 사용한다.
             */
            .rowMapper(new BeanPropertyRowMapper<>(Pay.class))
            /**
             * Reader 로 사용할 쿼리문
             */
            .sql("SELECT id, amount, tx_name, tx_date_time FROM pay")
            /**
             * reader 의 이름 지정
             * Bean 의 이름이 아니며, Spring Batch 의 ExecutionContext 에서 저장되어질 이름
             */
            .name("jdbcCursorItemReader")
            .build();
  }

  private ItemWriter<Pay> jdbcCursorItemWriter() {
    return list -> {
      for (Pay pay : list) {
        log.info("** Current Pay = {}", pay);
      }
    };
  }

}
