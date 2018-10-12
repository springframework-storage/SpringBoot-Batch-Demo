package com.example.batch.job.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * '@JobScope' 는 Step 선언문에서 사용 가능
 * '@StepScope' 는 Tasklet, ItemReader, ItemWriter, ItemProcessor 에서 사용 가능
 *
 * 현재 Job Parameter 의 타입으로 사용할 수 있는 것은 Double, Long, Date, String 이 있습니다.
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScopeJobFirstConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job scopeJob() {
    return jobBuilderFactory.get("scopeJob")
            /**
             * 호출 시 null 을 할당하는 것은 Job Parameter 의 할당이 어플리케이션 실행 시에 일어나지 않기 때문입니다.
             */
            .start(scopeStep1(null))
            .next(scopeStep2())
            .build();
  }

  @Bean
  @JobScope
  public Step scopeStep1(@Value("#{jobParameters[requestDate]}") String requestDate) {
    return stepBuilderFactory.get("scopeStep1")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> This is scopeStep1");
              log.info(">>>>> requestDate = {}", requestDate);
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  public Step scopeStep2() {
    return stepBuilderFactory.get("scopeStep2")
            .tasklet(scopeStep2Tasklet(null))
            .build();
  }

  /**
   * Spring Batch 컴포넌트 (Tasklet, ItemReader, ItemWriter, ItemProcessor 등)에 '@StepScope'를 사용하면
   * Spring Batch 가 Spring 컨테이너를 통해 지정된 Step 의 실행 시점에 해당 컴포넌트를 Spring Bean 으로 생성합니다.
   *
   * '@JobScope'는 Job 실행 시점에 Bean 이 생성됩니다.
   * 즉, Bean 의 생성 시점을 지정된 Scope 가 실행되는 시점으로 지연시킵니다.
   *
   * @param requestDate
   * @return
   */
  @Bean
  @StepScope
  public Tasklet scopeStep2Tasklet(@Value("#{jobParameters[requestDate]}") String requestDate) {
    return (contribution, chunkContext) -> {
      log.info(">>>>> This is scopeStep2");
      log.info(">>>>> requestDate = {}", requestDate);
      return RepeatStatus.FINISHED;
    };
  }

}
