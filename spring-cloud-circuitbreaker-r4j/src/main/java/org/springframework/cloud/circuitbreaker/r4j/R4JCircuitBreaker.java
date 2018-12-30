/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.circuitbreaker.r4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.cloud.circuitbreaker.commons.CircuitBreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.vavr.control.Try;


/**
 * @author Ryan Baxter
 */
public class R4JCircuitBreaker implements CircuitBreaker {

	private String id;
	private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig;
	private CircuitBreakerRegistry registry;
	private TimeLimiterConfig timeLimiterConfig;
	private R4JCircuitBreakerCustomizer r4JCircuitBreakerCustomizer;
	private ExecutorService executorService;

	public R4JCircuitBreaker(String id,
			io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig,
			TimeLimiterConfig timeLimiterConfig,
			CircuitBreakerRegistry circuitBreakerRegistry,
			R4JCircuitBreakerCustomizer r4JCircuitBreakerCustomizer,
			ExecutorService executorService) {
		this.id = id;
		this.circuitBreakerConfig = circuitBreakerConfig;
		this.registry = circuitBreakerRegistry;
		this.timeLimiterConfig = timeLimiterConfig;
		this.r4JCircuitBreakerCustomizer = r4JCircuitBreakerCustomizer;
		this.executorService = executorService;
	}

	@Override
	public <T> T run(Supplier<T> toRun) {
		TimeLimiter timeLimiter = TimeLimiter.of(timeLimiterConfig);
		Supplier<Future<T>> futureSupplier = () -> executorService.submit(toRun::get);
		Callable restrictedCall = TimeLimiter
				.decorateFutureSupplier(timeLimiter, futureSupplier);

		io.github.resilience4j.circuitbreaker.CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker(id, circuitBreakerConfig);
		r4JCircuitBreakerCustomizer.customize(defaultCircuitBreaker);
		Callable<T> callable = io.github.resilience4j.circuitbreaker.CircuitBreaker
				.decorateCallable(defaultCircuitBreaker, restrictedCall);
		return Try.of(callable::call).get();
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		TimeLimiter timeLimiter = TimeLimiter.of(timeLimiterConfig);
		Supplier<Future<T>> futureSupplier = () -> executorService.submit(toRun::get);
		Callable restrictedCall = TimeLimiter
				.decorateFutureSupplier(timeLimiter, futureSupplier);

		io.github.resilience4j.circuitbreaker.CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker(id, circuitBreakerConfig);
		r4JCircuitBreakerCustomizer.customize(defaultCircuitBreaker);
		Callable<T> callable = io.github.resilience4j.circuitbreaker.CircuitBreaker
				.decorateCallable(defaultCircuitBreaker, restrictedCall);
		return Try.of(callable::call).recover(fallback).get();
	}
}
