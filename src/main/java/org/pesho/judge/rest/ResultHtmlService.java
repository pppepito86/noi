package org.pesho.judge.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@PreAuthorize("hasRole('RESULT')")
@Controller
public class ResultHtmlService extends HtmlService {

	@GetMapping("/results")
	public String adminResultsPage(Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions().stream()
				.filter(x -> !"author".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"admin".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"test".equalsIgnoreCase(x.get("city").toString()))
				.collect(Collectors.toList());
		int problemsCount = repository.maxProblemNumber();
		Map<String, Map<String, Object>> totals = new HashMap<>();
		for (Map<String, Object> submission: submissions) {
			String key = submission.get("username").toString().toUpperCase() +
					submission.get("city").toString().toUpperCase() +
					submission.get("contest_name").toString().toUpperCase();
			submission.put("key", key);
			HashMap<String, Object> info = new HashMap<>();
			info.put("username", submission.get("username").toString().toUpperCase());
			info.put("city", submission.get("city").toString().toUpperCase());
			info.put("contest_name", submission.get("contest_name").toString().toUpperCase());
			totals.put(key, info);
		}
		List<Map<String,Object>> users = repository.listUsers();
		for (Map<String, Object> user: users) {
			if (user.get("name") == null || user.get("city") == null) continue;
			String key = user.get("name").toString().toUpperCase() +
					user.get("city").toString().toUpperCase() +
					user.get("contest").toString().toUpperCase();
			if (totals.containsKey(key)) {
				String displayName = Optional.ofNullable(user.get("display_name")).map(Object::toString).orElse("");
				String grade = Optional.ofNullable(user.get("grade")).map(Object::toString).orElse("");
				String school = Optional.ofNullable(user.get("school")).map(Object::toString).orElse("");
				totals.get(key).put("display_name", displayName);
				totals.get(key).put("grade", grade);
				totals.get(key).put("school", school);
			} else {
				String username = Optional.ofNullable(user.get("name")).map(Object::toString).orElse("");
				String displayName = Optional.ofNullable(user.get("display_name")).map(Object::toString).orElse("");
				String grade = Optional.ofNullable(user.get("grade")).map(Object::toString).orElse("");
				String school = Optional.ofNullable(user.get("school")).map(Object::toString).orElse("");
				String city = Optional.ofNullable(user.get("city")).map(Object::toString).orElse("");
				String contest = Optional.ofNullable(user.get("contest")).map(Object::toString).orElse("");
				Map<String,Object> userInfo = new HashMap<>();
				userInfo.put("username", username);
				userInfo.put("display_name", displayName);
				userInfo.put("grade", grade);
				userInfo.put("school", school);
				userInfo.put("city", city);
				userInfo.put("contest_name", contest);
				totals.put(key, userInfo);
			}
			totals.get(key).put("total", 0);
		}
		
		Map<String, List<Map<String, Object>>> usersSubmissions = submissions.stream().collect(Collectors.groupingBy(s -> s.get("key").toString()));
		Map<String, Map<Integer, Map<String, Object>>> results = new HashMap<>();
		for (Map.Entry<String, List<Map<String, Object>>> userSubmissions: usersSubmissions.entrySet()) {
			Map<Integer, Map<String, Object>> userResults = fixSubmissions(userSubmissions.getValue(), problemsCount, true);
			results.put(userSubmissions.getKey(), userResults);
			
			int total = 0;
			for (int i = 1; i <= problemsCount; i++) {
				Integer points = (Integer) userResults.get(i).get("points");
				if (points == null) points = 0;
				total += points;
			}
			totals.get(userSubmissions.getKey()).put("total", total);
		}
		
		for (Map<String, Object> user: users) {
			if (user.get("name") == null || user.get("city") == null) continue;
			String key = user.get("name").toString().toUpperCase() +
					user.get("city").toString().toUpperCase() +
					user.get("contest").toString().toUpperCase();
			if (results.containsKey(key)) continue;
			
			Map<Integer, Map<String, Object>> map = new TreeMap<>();
			for (int i = 1; i <= problemsCount; i++) {
				HashMap<String, Object> emptyMap = new HashMap<>();
				emptyMap.put("points", 0);
				emptyMap.put("verdict", "not solved");
				map.put(i, emptyMap);
			}
			results.put(key, map);
		}
		
		model.addAttribute("contests", contests);
		model.addAttribute("results", results);
		model.addAttribute("totals", totals);
		
		List<String> problems = new ArrayList<>(problemsCount);
		for (int i = 1; i <= problemsCount; i++) problems.add("Problem " + i);
		model.addAttribute("problems", problems);
		return "/results/results";
	}

	@GetMapping("/resultsfull")
	public String adminResultsFullPage(Model model) {
		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions().stream()
				.filter(x -> !"author".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"admin".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"test".equalsIgnoreCase(x.get("city").toString()))
				.collect(Collectors.toList());
		int problemsCount = repository.maxProblemNumber();
		Map<String, Map<String, Object>> totals = new HashMap<>();
		for (Map<String, Object> submission: submissions) {
			String key = submission.get("username").toString().toUpperCase() +
					submission.get("city").toString().toUpperCase() +
					submission.get("contest_name").toString().toUpperCase();
			submission.put("key", key);
			HashMap<String, Object> info = new HashMap<>();
			info.put("username", submission.get("username").toString().toUpperCase());
			info.put("city", submission.get("city").toString().toUpperCase());
			info.put("contest_name", submission.get("contest_name").toString().toUpperCase());
			totals.put(key, info);
		}
		List<Map<String,Object>> users = repository.listUsers();
		for (Map<String, Object> user: users) {
			if (user.get("name") == null || user.get("city") == null) continue;
			String key = user.get("name").toString().toUpperCase() +
					user.get("city").toString().toUpperCase() +
					user.get("contest").toString().toUpperCase();
			if (totals.containsKey(key)) {
				String displayName = Optional.ofNullable(user.get("display_name")).map(Object::toString).orElse("");
				String grade = Optional.ofNullable(user.get("grade")).map(Object::toString).orElse("");
				String school = Optional.ofNullable(user.get("school")).map(Object::toString).orElse("");
				totals.get(key).put("name", displayName);
				totals.get(key).put("grade", grade);
				totals.get(key).put("school", school);
			} else {
				String username = Optional.ofNullable(user.get("name")).map(Object::toString).orElse("");
				String displayName = Optional.ofNullable(user.get("display_name")).map(Object::toString).orElse("");
				String grade = Optional.ofNullable(user.get("grade")).map(Object::toString).orElse("");
				String school = Optional.ofNullable(user.get("school")).map(Object::toString).orElse("");
				String city = Optional.ofNullable(user.get("city")).map(Object::toString).orElse("");
				String contest = Optional.ofNullable(user.get("contest")).map(Object::toString).orElse("");
				Map<String,Object> userInfo = new HashMap<>();
				userInfo.put("username", username);
				userInfo.put("name", displayName);
				userInfo.put("grade", grade);
				userInfo.put("school", school);
				userInfo.put("city", city);
				userInfo.put("contest_name", contest);
				totals.put(key, userInfo);
			}
			totals.get(key).put("total", 0);
		}

		Map<String, List<Map<String, Object>>> usersSubmissions = submissions.stream().collect(Collectors.groupingBy(s -> s.get("key").toString()));
		Map<String, Map<Integer, Map<String, Object>>> results = new HashMap<>();
		for (Map.Entry<String, List<Map<String, Object>>> userSubmissions: usersSubmissions.entrySet()) {
			Map<Integer, Map<String, Object>> userResults = fixSubmissions(userSubmissions.getValue(), problemsCount, true);
			results.put(userSubmissions.getKey(), userResults);
			
			int total = 0;
			for (int i = 1; i <= problemsCount; i++) {
				Integer points = (Integer) userResults.get(i).get("points");
				if (points == null) points = 0;
				total += points;
			}
			totals.get(userSubmissions.getKey()).put("total", total);
		}
		
		for (Map<String, Object> user: users) {
			if (user.get("name") == null || user.get("city") == null) continue;
			String key = user.get("name").toString().toUpperCase() +
					user.get("city").toString().toUpperCase() +
					user.get("contest").toString().toUpperCase();
			if (results.containsKey(key)) continue;
			
			Map<Integer, Map<String, Object>> map = new TreeMap<>();
			for (int i = 1; i <= problemsCount; i++) {
				HashMap<String, Object> emptyMap = new HashMap<>();
				emptyMap.put("points", 0);
				emptyMap.put("verdict", "not solved");
				map.put(i, emptyMap);
			}
			results.put(key, map);
		}
		
		model.addAttribute("contests", contests);
		model.addAttribute("results", results);
		model.addAttribute("totals", totals);
		
		List<String> problems = new ArrayList<>(problemsCount);
		for (int i = 1; i <= problemsCount; i++) problems.add("Problem " + i);
		model.addAttribute("problems", problems);
		return "resultsfull";
	}
	
	@GetMapping("/csv/{contest}/{city}")
	public ResponseEntity<InputStreamResource> adminResultsFullCsvPage(
			@PathVariable("contest") String contest,
			@PathVariable("city") String grad) {
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions().stream()
				.filter(x -> !"author".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"admin".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"test".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> grad.equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> contest.equalsIgnoreCase(x.get("contest_name").toString()))
				.collect(Collectors.toList());
		int problemsCount = repository.maxProblemNumber();
		Map<String, Map<String, Object>> totals = new HashMap<>();
		for (Map<String, Object> submission: submissions) {
			String key = submission.get("username").toString().toUpperCase() +
					submission.get("city").toString().toUpperCase() +
					submission.get("contest_name").toString().toUpperCase();
			submission.put("key", key);
			HashMap<String, Object> info = new HashMap<>();
			info.put("username", submission.get("username").toString().toUpperCase());
			info.put("city", submission.get("city").toString().toUpperCase());
			info.put("contest_name", submission.get("contest_name").toString().toUpperCase());
			totals.put(key, info);
		}
		List<Map<String,Object>> users = repository.listUsers();
		for (Map<String, Object> user: users) {
			if (user.get("name") == null || user.get("city") == null) continue;
			if (!grad.equalsIgnoreCase(user.get("city").toString())) continue;
			String key = user.get("name").toString().toUpperCase() +
					user.get("city").toString().toUpperCase() +
					user.get("contest").toString().toUpperCase();
			if (!contest.equalsIgnoreCase(user.get("contest").toString())) continue;
			
			if (totals.containsKey(key)) {
				String displayName = Optional.ofNullable(user.get("display_name")).map(Object::toString).orElse("");
				String grade = Optional.ofNullable(user.get("grade")).map(Object::toString).orElse("");
				String school = Optional.ofNullable(user.get("school")).map(Object::toString).orElse("");
				totals.get(key).put("name", displayName);
				totals.get(key).put("grade", grade);
				totals.get(key).put("school", school);
			} else {
				String username = Optional.ofNullable(user.get("name")).map(Object::toString).orElse("");
				String displayName = Optional.ofNullable(user.get("display_name")).map(Object::toString).orElse("");
				String grade = Optional.ofNullable(user.get("grade")).map(Object::toString).orElse("");
				String school = Optional.ofNullable(user.get("school")).map(Object::toString).orElse("");
				String city = Optional.ofNullable(user.get("city")).map(Object::toString).orElse("");
				Map<String,Object> userInfo = new HashMap<>();
				userInfo.put("username", username);
				userInfo.put("name", displayName);
				userInfo.put("grade", grade);
				userInfo.put("school", school);
				userInfo.put("city", city);
				userInfo.put("contest_name", contest);
				totals.put(key, userInfo);
			}
			totals.get(key).put("total", 0);
		}
		
		Map<String, List<Map<String, Object>>> usersSubmissions = submissions.stream().collect(Collectors.groupingBy(s -> s.get("key").toString()));
		Map<String, Map<Integer, Map<String, Object>>> results = new HashMap<>();
		for (Map.Entry<String, List<Map<String, Object>>> userSubmissions: usersSubmissions.entrySet()) {
			Map<Integer, Map<String, Object>> userResults = fixSubmissions(userSubmissions.getValue(), problemsCount, true);
			results.put(userSubmissions.getKey(), userResults);
			
			int total = 0;
			for (int i = 1; i <= problemsCount; i++) {
				Integer points = (Integer) userResults.get(i).get("points");
				if (points == null) points = 0;
				total += points;
			}
			totals.get(userSubmissions.getKey()).put("total", total);
		}
		
		for (Map<String, Object> user: users) {
			if (user.get("name") == null || user.get("city") == null) continue;
			if (!grad.equalsIgnoreCase(user.get("city").toString())) continue;

			String key = user.get("name").toString().toUpperCase() +
					user.get("city").toString().toUpperCase() +
					user.get("contest").toString().toUpperCase();
			if (results.containsKey(key)) continue;
			if (!contest.equalsIgnoreCase(user.get("contest").toString())) continue;
			
			Map<Integer, Map<String, Object>> map = new TreeMap<>();
			for (int i = 1; i <= problemsCount; i++) {
				HashMap<String, Object> emptyMap = new HashMap<>();
				emptyMap.put("points", 0);
				emptyMap.put("verdict", "not solved");
				map.put(i, emptyMap);
			}
			results.put(key, map);
		}

		HttpHeaders respHeaders = new HttpHeaders();
	    respHeaders.setContentDispositionFormData("attachment", "results_" + contest + ".csv");
	    
	    StringBuffer buffer = new StringBuffer();
	    
	    final String[] displayColumnNames = new String[] {
	    		"initials", "name", "city", "school", "grade", "total", 
	    		"problem 1", "problem 2", "problem 3", "details 1", "details 2", "details 3"};
	    
	    try (
            CSVPrinter csvPrinter = new CSVPrinter(buffer, CSVFormat.DEFAULT
                    .withHeader(displayColumnNames));
        ) {
	    	for (Map.Entry<String, Map<Integer, Map<String, Object>>> result : results.entrySet()) {
	    		Map<String, Object> info = totals.get(result.getKey());
	    		ArrayList<Object> list = new ArrayList<>();
	    		list.add(info.get("username"));
	    		list.add(info.get("name"));
	    		list.add(info.get("city"));
	    		list.add(info.get("school"));
	    		list.add(info.get("grade"));
	    		list.add(info.get("total"));

	    		for (int i = 1; i <= 3; i++) {
		    		list.add(result.getValue().get(i).get("points"));
	    		}
	    		for (int i = 1; i <= 3; i++) {
	    			list.add(result.getValue().get(i).get("details"));
	    		}
	    		
//	    		Object[] values = Arrays
//	    				.stream(dbColumns)
//	    				.map(column -> user.get(column))
//	    				.toArray();
	    		csvPrinter.printRecord(list.toArray());
	    	}
            csvPrinter.flush();  
        } catch (IOException e) {
			e.printStackTrace();
		}
	    
	    InputStream is = new ByteArrayInputStream(buffer.toString().getBytes());
		InputStreamResource inputStreamResource = new InputStreamResource(is);
	    
		return new ResponseEntity<InputStreamResource>(inputStreamResource, 
	    		respHeaders, HttpStatus.OK);
	}
	
	
	@GetMapping("/results/{contest}")
	public String adminContestResultsPage(@PathVariable("contest") String contest, Model model) {		List<Map<String,Object>> contests = repository.listContests();
		List<Map<String,Object>> submissions = repository.listDetailedSubmissions().stream()
				.filter(x -> !"author".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"admin".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> !"test".equalsIgnoreCase(x.get("city").toString()))
				.filter(x -> contest.equalsIgnoreCase(x.get("contest_name").toString()))
				.collect(Collectors.toList());
		int problemsCount = repository.maxProblemNumber();
		Map<String, Map<String, Object>> totals = new HashMap<>();
		for (Map<String, Object> submission: submissions) {
			String key = submission.get("username").toString().toUpperCase() +
					submission.get("city").toString().toUpperCase() +
					submission.get("contest_name").toString().toUpperCase();
			submission.put("key", key);
			HashMap<String, Object> info = new HashMap<>();
			info.put("username", submission.get("username").toString().toUpperCase());
			info.put("city", submission.get("city").toString().toUpperCase());
			info.put("contest_name", submission.get("contest_name").toString().toUpperCase());
			totals.put(key, info);
		}
		List<Map<String,Object>> users = repository.listUsers();
		for (Map<String, Object> user: users) {
			if (user.get("name") == null || user.get("city") == null) continue;
			String key = user.get("name").toString().toUpperCase() +
					user.get("city").toString().toUpperCase() +
					user.get("contest").toString().toUpperCase();
			if (!contest.equalsIgnoreCase(user.get("contest").toString())) continue;

			if (totals.containsKey(key)) {
				String displayName = Optional.ofNullable(user.get("display_name")).map(Object::toString).orElse("");
				String grade = Optional.ofNullable(user.get("grade")).map(Object::toString).orElse("");
				String school = Optional.ofNullable(user.get("school")).map(Object::toString).orElse("");
				totals.get(key).put("display_name", displayName);
				totals.get(key).put("grade", grade);
				totals.get(key).put("school", school);
			} else {
				String username = Optional.ofNullable(user.get("name")).map(Object::toString).orElse("");
				String displayName = Optional.ofNullable(user.get("display_name")).map(Object::toString).orElse("");
				String grade = Optional.ofNullable(user.get("grade")).map(Object::toString).orElse("");
				String school = Optional.ofNullable(user.get("school")).map(Object::toString).orElse("");
				String city = Optional.ofNullable(user.get("city")).map(Object::toString).orElse("");
				Map<String,Object> userInfo = new HashMap<>();
				userInfo.put("username", username);
				userInfo.put("display_name", displayName);
				userInfo.put("grade", grade);
				userInfo.put("school", school);
				userInfo.put("city", city);
				userInfo.put("contest_name", contest);
				totals.put(key, userInfo);
			}
			totals.get(key).put("total", 0);
		}
		
		Map<String, List<Map<String, Object>>> usersSubmissions = submissions.stream().collect(Collectors.groupingBy(s -> s.get("key").toString()));
		Map<String, Map<Integer, Map<String, Object>>> results = new HashMap<>();
		for (Map.Entry<String, List<Map<String, Object>>> userSubmissions: usersSubmissions.entrySet()) {
			Map<Integer, Map<String, Object>> userResults = fixSubmissions(userSubmissions.getValue(), problemsCount, true);
			results.put(userSubmissions.getKey(), userResults);
			
			int total = 0;
			for (int i = 1; i <= problemsCount; i++) {
				Integer points = (Integer) userResults.get(i).get("points");
				if (points == null) points = 0;
				total += points;
			}
			totals.get(userSubmissions.getKey()).put("total", total);
		}
		
		for (Map<String, Object> user: users) {
			if (user.get("name") == null || user.get("city") == null) continue;
			String key = user.get("name").toString().toUpperCase() +
					user.get("city").toString().toUpperCase() +
					user.get("contest").toString().toUpperCase();
			if (results.containsKey(key)) continue;
			if (!contest.equalsIgnoreCase(user.get("contest").toString())) continue;
			
			Map<Integer, Map<String, Object>> map = new TreeMap<>();
			for (int i = 1; i <= problemsCount; i++) {
				HashMap<String, Object> emptyMap = new HashMap<>();
				emptyMap.put("points", 0);
				emptyMap.put("verdict", "not solved");
				map.put(i, emptyMap);
			}
			results.put(key, map);
		}
		
		model.addAttribute("contests", contests);
		model.addAttribute("results", results);
		model.addAttribute("totals", totals);
		
		List<String> problems = new ArrayList<>(problemsCount);
		for (int i = 1; i <= problemsCount; i++) problems.add("Problem " + i);
		model.addAttribute("problems", problems);
		return "results/results";
	}

}
