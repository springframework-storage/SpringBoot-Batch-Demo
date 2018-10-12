package com.example.batch.job.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StepNextConditionalJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job stepNextConditionalJob() {
    return jobBuilderFactory.get("stepNextConditionalJob")
            .start(conditionalJobStep1())
            .on("FAILED") // FAILED 일 경우
            .to(conditionalJobStep3())  // step3 으로 이동
            .on("*")  // step3 의 결과에 관계 없이
            .end()  // step3 으로 이동하면 Flow 종료
            .from(conditionalJobStep1())  // step1 로부터
            .on("*")  // FAILED 외 모든 경우
            .to(conditionalJobStep2())  // step2 로 이동
            .next(conditionalJobStep3())  // step2 가 정상 종료되면, step3 으로 이동
            .on("*")  // step3 의 결과에 관계 없이
            .end()  // step3 으로 이동하면 Flow 종료
            .end()  // Job 종료
            .build();
  }

  @Bean
  public Step conditionalJobStep1() {
    return stepBuilderFactory.get("step1")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> This is stepNextConditionalJob Step1");

              /**
               * ExitStatus 를 FAILED 로 지정
               * 해당 status 를 보고 flow 가 진행된다.
               */
//              contribution.setExitStatus(ExitStatus.FAILED);
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  public Step conditionalJobStep2() {
    return stepBuilderFactory.get("conditionalJobStep2")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> This is stepNextConditionalJob Step2");
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  public Step conditionalJobStep3() {
    return stepBuilderFactory.get("conditionalJobStep3")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> This is stepNextConditionalJob Step3");
              return RepeatStatus.FINISHED;
            })
            .build();
  }


}
