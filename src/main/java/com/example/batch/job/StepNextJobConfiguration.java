package com.example.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * next()는 순차적으로 Step 들을 연결시킬 때 사용
 * step1 -> step2 -> step3 순으로 하나씩 실행시킬 때 next()는 좋은 방법
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StepNextJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job stepNextJob() {
    return jobBuilderFactory.get("stepNextJob")
            .start(step1())
            .next(step2())
            .next(step3())
            .build();
  }

  @Bean
  public Step step1() {
    return stepBuilderFactory.get("step1")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> This is Step1");
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  public Step step2() {
    return stepBuilderFactory.get("step2")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> This is Step2");
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  public Step step3() {
    return stepBuilderFactory.get("step3")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> This is Step3");
              return RepeatStatus.FINISHED;
            })
            .build();
  }

}
