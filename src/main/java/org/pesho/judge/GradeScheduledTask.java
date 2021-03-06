package org.pesho.judge;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.pesho.judge.repositories.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GradeScheduledTask {

    @Value("${work.dir}")
    private String workDir;
	
	@Autowired
	private Repository repository;
	
	@Autowired
	private WorkersQueue queue;
	
	private ObjectMapper mapper = new ObjectMapper();
	
    @Scheduled(fixedDelay = 1000)
	public void gradeTask() throws IOException {
    	List<Map<String, Object>> submissions = repository.submissionsToGrade();
    	submissions.addAll(repository.failedSubmissions());
    	for (Map<String, Object> submission: submissions) {
    		boolean graded = grade(submission);
    		if (!graded) return;
    	}
    }

	private boolean grade(Map<String, Object> submission) throws IOException {
		int submissionId = (int) submission.get("id");
		int problemId = (int) submission.get("problem_id");
		
		Optional<Map<String,Object>> maybeProblem = repository.getProblem(problemId);
		if (!maybeProblem.isPresent()) {
			repository.addStatus(submissionId, "problem not available");
			return true;
		}
		
		Optional<Worker> worker = queue.take();
		if (!worker.isPresent()) return false;

		System.out.println("Starting " + worker.get().getUrl() + " for " + submissionId + ", " + problemId);
		repository.addStatus(submissionId, "judging");
			
		long sTime = System.currentTimeMillis();
		
		File sourceFile = new File(new File(workDir).getAbsolutePath() + File.separator + "submissions" + File.separator + submissionId
				+ File.separator + submission.get("file"));

		worker.get().setFree(false);
		Runnable runnable = () -> {
			String result = "";
			String details = "";
			int points = 0;
			try {
				if (sourceFile.length() > 64 * 1024) {
					result = "LF";
				} else {
					SubmissionScore score = worker.get().grade(maybeProblem.get(), submission, workDir, sourceFile);
					details = mapper.writeValueAsString(score);
					points = (int) Math.round(score.getScore());
					StepResult[] values = score.getScoreSteps().values().toArray(new StepResult[0]);
					if (values.length > 1) {
						for (int i = 1; i < values.length; i++) {
							StepResult step = values[i];
							if (i != 1)
								result += ",";
							result += step.getVerdict();
						}
					} else {
						result = values[0].getVerdict().toString();
						System.out.println("Submission <" + submissionId + "> failed with " + values[0].getReason());
					}
				}
			} catch (Exception e) {
				System.out.println("Failed judging for submission: " + submissionId);
				e.printStackTrace();
				result = "system error";
			} finally {
				System.out.println("Finishing " + worker.get().getUrl() + " " + submission.get("id") + ", " + problemId + " time - " + (System.currentTimeMillis() - sTime)/1000);
				worker.get().setFree(true);
				System.out.println("Judging " + worker.get().getUrl() + " " + result + " " + submissionId + " " + problemId);
				System.out.println("Scoring " + worker.get().getUrl() + " " + submissionId + " " + problemId);
				
				if (submission.get("points") != null) {
					int previousPoints = (int) submission.get("points");
					String diff = previousPoints + " ->" + points;
					if (points > previousPoints) {
						repository.addLog("rejudge", "points increased("+diff+") for " + submissionId, "");
					} else if (points < previousPoints) {
						repository.addLog("rejudge", "points decreased("+diff+") for " + submissionId, "");
					}
				}
				
				if (submission.get("city") != null && submission.get("username") != null) {
					String city = submission.get("city").toString();
					String username = submission.get("username").toString();
					
					List<Map<String, Object>> similarSubmissions = repository.lastSimilarSubmission(problemId, username, city);
					if (similarSubmissions.size() > 1) {
						Map<String, Object> similar = similarSubmissions.get(1);
						if (similar.get("points") != null) {
							int similarPoints = (int) similar.get("points");
							if (similarPoints != points) {
								String diff = similarPoints + " ->" + points;
								repository.addLog("new score", "points changed("+diff+") for " + submissionId, "");
							}
						}
					}
				}
				
				repository.addScore(submissionId, result, details, points);
				if (result.equals("CE")) {
					repository.addLog("grade", "compilation error " + submissionId, "");
				}
				if (result.equals("LF")) {
					repository.addLog("grade", "large file " + submissionId, "");
				}
				if (result.equals("SE")) {
					repository.addLog("grade", "system error " + submissionId, "");
				}
			}
		};
			
		new Thread(runnable).start();
		return true;
	}
}
