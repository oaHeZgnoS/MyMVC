package com.szh.mymvc.service.impl;

import com.szh.mymvc.annotation.MyService;
import com.szh.mymvc.service.TestService;

@MyService("TestServiceImpl")
public class TestServiceImpl implements TestService {

	public String query(String name, String age) {
		return "name = " + name + ", age = " + age;
	}

}
