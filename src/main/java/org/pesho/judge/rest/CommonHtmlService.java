package org.pesho.judge.rest;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommonHtmlService extends HtmlService {

    @GetMapping("/")
    public String index() {
    	Object adminRole = new SimpleGrantedAuthority("ROLE_ADMIN");
    	Object resultsRole = new SimpleGrantedAuthority("ROLE_RESULT");
		boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(adminRole);
		boolean isResult = SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(resultsRole);
    	if (isAdmin) {
    		return "redirect:/admin";
    	} else if (isResult) {
    		return "redirect:/results";
    	} else {
    		return "redirect:/user/problem/1";
    	}
    }

    @GetMapping("/login")
    public String login() {
    	return "/login";
    }

//	@GetMapping("/home")
//    public String table(@RequestParam(value = "city") Optional<String> maybeCity, Model model) {
//    	if (!maybeCity.isPresent()) {
//    		return "home";
//    	}
//    	
//    	String city = maybeCity.get().toLowerCase();
//    	if (city.isEmpty()) {
//    		return "home";
//    	}
//    	
//    	
//    	List<Map<String,Object>> submissions = repository.listCitySubmissions(city);
//    	if (submissions.size() > 0) {
//    		Map<String, Map<String, List<Map<String, Object>>>> result = new HashMap<>();
//    		for (String contest: new String[] {"A", "B", "C", "D", "E"}) {
//    			List<Map<String, Object>> contestSubmissions = repository.listContestSubmissions(city, contest);
//        		Map<String, List<Map<String, Object>>> usersSubmissions = contestSubmissions.stream().collect(Collectors.groupingBy(s->s.get("username").toString()));
//        		for (List<Map<String, Object>> list: usersSubmissions.values()) {
//        			boolean[] numbers = new boolean[4];
//        			for (Map<String, Object> s: list) numbers[Integer.valueOf(s.get("number").toString())]=true;
//        			for (int i = 1; i <= 3; i++) {
//        				if (numbers[i]) continue;
//        				HashMap<String, Object> m = new HashMap<>();
//        				m.put("number", i);
//        				list.add(m);
//        			}
//        			
//        			Collections.sort(list, new Comparator<Map<String, Object>>() {
//        				@Override
//        				public int compare(Map<String, Object> o1, Map<String, Object> o2) {
//        					return Integer.valueOf(o1.get("number").toString()) - Integer.valueOf(o2.get("number").toString());
//        				}
//					});
//        		}
//        		result.put(contest, usersSubmissions);
//    		}
//    		
//    		System.out.println(result);
//    		model.addAttribute("contests", result);
//        	return "result";
//        } else {
//        	model.addAttribute("city", city);
//        	return "upload";
//        }
//    }

//    @PostMapping("/home/grade")
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
//	public synchronized String addSubmission(@RequestPart("file") MultipartFile file, 
//			@RequestParam("city") String city, Model model)
//			throws Exception {
//		File zipFile = getFile("temp", city, city + ".zip");
//		zipFile.getParentFile().mkdirs();
//		
//		FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
//		File zipFolder = getFile("temp", city, city);
//		unzip(zipFile, zipFolder);
//
//		List<File> listSourceFiles = listSourceFiles(zipFolder);
//		for (File sourceFile: listSourceFiles) {
//			String username = sourceFile.getParentFile().getName();
//			String contest = sourceFile.getParentFile().getParentFile().getName();
//			
//			contest = new HomographTranslator().translate(contest);
//			
//			String problemName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
//			problemName = new HomographTranslator().translate(problemName);
//			
//			String fileName = sourceFile.getName();
//			int submissionId = repository.addSubmission(city, username, contest, problemName, fileName);
//			if (submissionId != 0) {
//				File newFile = getFile("submissions", String.valueOf(submissionId), fileName);
//				newFile.getParentFile().mkdirs();
//				FileUtils.copyFile(sourceFile, newFile);
//			} else {
//				String details = String.format("%s_%s_%s_%s", city, contest, username, problemName);
//				repository.addLog("submission", "problem not found for " + details, "");
//			}
//		}
//		
//		FileUtils.deleteQuietly(zipFile);
//		FileUtils.deleteQuietly(zipFolder);
//		
//		return "redirect:/home?city="+city;
//	}

    
}
