package com.szh.mymvc.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.szh.mymvc.annotation.MyAutowired;
import com.szh.mymvc.annotation.MyController;
import com.szh.mymvc.annotation.MyRequestMapping;
import com.szh.mymvc.annotation.MyRequestParam;
import com.szh.mymvc.service.TestService;

@MyController
@MyRequestMapping("/ctr1")
public class TestController {
	@MyAutowired("TestServiceImpl")
	private TestService testService;

	@MyRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp, @MyRequestParam("name") String name,
			@MyRequestParam("age") String age) {
		PrintWriter pw;
		try {
			pw = resp.getWriter();
			String result = testService.query(name, age);
			pw.write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@MyRequestMapping("/test")
	public void test(HttpServletRequest req, HttpServletResponse resp) {
		PrintWriter pw;
		try {
			pw = resp.getWriter();
			String result = "I am test!";
			pw.write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
