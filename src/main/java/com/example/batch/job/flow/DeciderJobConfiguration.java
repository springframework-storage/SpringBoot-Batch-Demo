package com.example.batch.job.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeciderJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job deciderJob() {
    return jobBuilderFactory.get("deciderJob")
            .start(startStep())
            .next(decider())  // 홀수, 짝수 판단
            .from(decider())  // decider 의 상태가
            .on("ODD")  // 홀수라면
            .to(oddStep())  // oddStep
            .from(decider())  // decider 의 상태가
            .on("EVEN") // 짝수라면
            .to(evenStep()) // evenStep
            .end()  // builder 종료
            .build();
  }

  @Bean
  public Step startStep() {
    return stepBuilderFactory.get("startStep")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> Start!");
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  public Step evenStep() {
    return stepBuilderFactory.get("evenStep")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> 짝수입니다.");
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  public Step oddStep() {
    return stepBuilderFactory.get("oddStep")
            .tasklet((contribution, chunkContext) -> {
              log.info(">>>>> 홀수입니다.");
              return RepeatStatus.FINISHED;
            })
            .build();
  }

  @Bean
  public JobExecutionDecider decider() {
    return new OddDecider();
  }

  public static class OddDecider implements JobExecutionDecider {

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
      Random random = new Random();

      int randomNumber = random.nextInt(50) + 1;
      log.info("*** 랜덤 숫자: {}", randomNumber);

      /**
       * Step 으로 처리하는 것이 아니기 때문에 ExitStatus 가 아닌 FlowExecutionStatus 로 상태 관리
       */
      if (randomNumber % 2 == 0) {
        return new FlowExecutionStatus("EVEN");
      } else {
        return new FlowExecutionStatus("ODD");
      }
    }

  }

}
