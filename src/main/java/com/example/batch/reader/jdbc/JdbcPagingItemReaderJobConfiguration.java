package com.example.batch.reader.jdbc;

import com.example.batch.domain.Pay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * JdbcCursorItemReader 와 설정이 크게 다른 것: createQueryProvider()
 * - JdbcCursorItemReader 는 단순히 String 타입으로 쿼리를 생성
 * - PagingItemReader 는 PagingQueryProvider 를 통해 쿼리를 생성
 * 이렇게 하는 데는 큰 이유가 있다.
 *
 * 각 DB 에는 Paging 을 지원하는 자체적인 전략들이 존재한다.
 * 그렇기 떄문에 Spring Batch 는 각 DB 의 Paging 전략에 맞춰 구현되어야만 합니다.
 * - ex) MySqlPagingQueryProvider
 * - ex) OraclePagingQueryProvider
 * - ex) PostgresPagingQueryProvider
 * - ex) SqlitePagingQueryProvider
 *
 * 하지만 이렇게 되면 DB 마다 Provider 코드를 바꿔야 한다는 불편함이 존재한다.
 * 그래서 Spring Batch 에서는 SqlPagingQueryProviderFactoryBean 을 통해 DataSource 설정값을 보고
 * DB Provider 중 하나를 자동으로 선택한다. (Spring Batch 에서 공식 지원하는 방법)
 */

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcPagingItemReaderJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final DataSource dataSource;

  private static final int chunkSize = 10;

  @Bean
  public Job jdbcPagingItemReaderJob() throws Exception {
    return jobBuilderFactory.get("jdbcPagingItemReaderJob")
            .start(jdbcPagingItemReaderStep())
            .build();
  }

  @Bean
  public Step jdbcPagingItemReaderStep() throws Exception {
    return stepBuilderFactory.get("jdbcPagingItemReaderStep")
            .<Pay, Pay>chunk(chunkSize)
            .reader(jdbcPagingItemReader())
            .writer(jdbcPagingItemWriter())
            .build();
  }

  @Bean
  public JdbcPagingItemReader<Pay> jdbcPagingItemReader() throws Exception {
    Map<String, Object> parameterValues = new HashMap<>();
    parameterValues.put("amount", 2000);

    return new JdbcPagingItemReaderBuilder<Pay>()
            .fetchSize(chunkSize)
            .dataSource(dataSource)
            .rowMapper(new BeanPropertyRowMapper<>(Pay.class))
            .queryProvider(createQueryProvider())
            /**
             * 쿼리에 대한 매개 변수 값의 Map 을 지정
             * queryProvider.setWhereClause 를 보면 어떻게 변수를 사용하는 지 알 수 있다.
             * where 절에서 선언된 파라미터 변수명과 parameterValues 에서 선언된 파라미터 변수명이 일치해야 한다.
             *
             * 예전이 '?' 로 파라미터의 위치를 지정하고 1 부터 시작하여 각 파라미터 값을 할당시키는 방식이었다면,
             * 그에 비해 굉장히 명시적이고 실수할 여지가 줄어들었다.
             *
             */
            .parameterValues(parameterValues)
            .name("jdbcPagingItemReader")
            .build();
  }

  private ItemWriter<Pay> jdbcPagingItemWriter() {
    return list -> {
      for (Pay pay : list) {
        log.info("** Current Pay = {}", pay);
      }
    };
  }

  @Bean
  public PagingQueryProvider createQueryProvider() throws Exception {
    SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
    queryProvider.setDataSource(dataSource);  // DB 에 맞는 PagingQueryProvider 를 선택하기 위함
    queryProvider.setSelectClause("id, amount, tx_name, tx_date_time");
    queryProvider.setFromClause("from pay");
    queryProvider.setWhereClause("where amount >= :amount");

    Map<String, Order> sortKeys = new HashMap<>(1);
    sortKeys.put("id", Order.ASCENDING);

    queryProvider.setSortKeys(sortKeys);

    return queryProvider.getObject();
  }

}
