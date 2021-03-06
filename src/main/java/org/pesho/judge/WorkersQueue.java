package org.pesho.judge;

import java.util.LinkedHashSet;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.pesho.judge.repositories.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkersQueue {

	private LinkedHashSet<Worker> workers = new LinkedHashSet<>();
	
	@Autowired
	Repository repository;
	
	public WorkersQueue() {	
	}
	
	@PostConstruct
	public void init() {
		System.out.println(repository.toString());
		System.out.println(repository.listWorkers().toString());
		repository.listWorkers().stream()
			.map(x -> x.get("url").toString())
			.map(url -> new Worker(url))
			.forEach(worker -> put(worker));
	}
	
	public synchronized void put(Worker worker) {
		workers.add(worker);
	}
	
	public synchronized void remove(String url) {
		workers.removeIf(worker -> worker.getUrl().contains(url));
	}
	
	public synchronized Optional<Worker> take() {
		return workers.parallelStream().filter(w -> w.isFree() && w.isAlive()).findFirst();
	}
	
	public synchronized LinkedHashSet<Worker> getAll() {
		return workers;
	}
}
