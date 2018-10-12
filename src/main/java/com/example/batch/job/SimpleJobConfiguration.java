package com.example.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch의 모든 Job은 @Configuration으로 등록해서 사용한다.
 * - Job은 하나의 배치 작업 단위
 * - Job 안에는 여러 Step이 존재하고, Step 안에 Tasklet 혹은 Reader & Processor & Writer 묶음이 존재한다.
 *
 * Tasklet은 Step 안에서 단일로 수행될 커스텀한 기능들을 선언할 때 사용한다. (개발자가 지정한 커스텀한 기능을 위한 단위)
 * - Tasklet 하나와 Reader & Processor & Writer 한 묶음이 같은 레벨이다.
 * - 그러므로, Reader & Processor가 끝나고 Tasklet으로 마무리 짓는 등으로 구현할 수는 없다!
 */

@Slf4j
@RequiredArgsConstructor  // 생성자 DI를 위한 lombok 어노테이션
@Configuration
public class SimpleJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job simpleJob() {
    // simpleJob 이라는 Batch Job을 생성
    return jobBuilderFactory.get("simpleJob")
            .start(simpleStep1(null))
            .next(simpleStep2(null))
            .build();
  }

//  /**
//   * Batch Job 실패 Case
//   *
//   * @param requestDate
//   * @return
//   */
//  @Bean
//  @JobScope
//  public Step simpleStep1(@Value("#{jobParameters[requestDate]}") String requestDate) {
//    return stepBuilderFactory.get("simpleStep1")
//            .tasklet((contribution, chunkContext) -> {
//              throw new IllegalArgumentException("step1에서 실패합니다.");
//            })
//            .build();
//  }

  @Bean
  @JobScope
  public Step simpleStep1(@Value("#{jobParameters[requestDate]}") String requestDate) {
    // simpleStep1 이라는 Batch Step 생성
    return stepBuilderFactory.get("simpleStep1")
            .tasklet((contribution, chunkContext) -> {  // Step 안에서 수행될 기능 명시
              log.info(">>>>>> This is Step1");
              log.info(">>>>>> requestDate = {}", requestDate);
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  @JobScope
  public Step simpleStep2(@Value("#{jobParameters[requestDate]}") String requestDate) {
    // simpleStep1 이라는 Batch Step 생성
    return stepBuilderFactory.get("simpleStep2")
            .tasklet((contribution, chunkContext) -> {  // Step 안에서 수행될 기능 명시
              log.info(">>>>>> This is Step2");
              log.info(">>>>>> requestDate = {}", requestDate);
              return RepeatStatus.FINISHED;
            })
            .build();
  }

}
