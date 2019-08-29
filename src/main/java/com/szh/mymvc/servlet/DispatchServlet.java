package com.szh.mymvc.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.szh.mymvc.annotation.MyAutowired;
import com.szh.mymvc.annotation.MyController;
import com.szh.mymvc.annotation.MyRequestMapping;
import com.szh.mymvc.annotation.MyRequestParam;
import com.szh.mymvc.annotation.MyService;

public class DispatchServlet extends HttpServlet {

	List<String> classNames = new ArrayList<String>();
	Map<String, Object> beans = new HashMap<String, Object>();
	Map<String, Method> methods = new HashMap<String, Method>();
	// <load-on-startup>0</load-on-startup>
	// 0：Tomcat初始化(springmvc会创建各种bean到Map,装配autowire等)
	public void init(ServletConfig config) {
		// 扫描工程所有类com.szh.mymvc.controller.TestController Class<?>
		doScan("com.szh");
		// 实例化beans
		doInstance();
		// 注入对象
		doAutowired();
		// 处理路径映射,根据url定位方法
		urlMapping();
	}

	public void urlMapping() {
		for (Map.Entry<String, Object> entry : beans.entrySet()) {
			Object instance = entry.getValue();
			Class<? extends Object> clazz = instance.getClass();
			if (clazz.isAnnotationPresent(MyController.class)) {
				MyRequestMapping anno = clazz.getAnnotation(MyRequestMapping.class);
				String classUrl = anno.value();
				Method[] ms = clazz.getMethods();
				for (Method method : ms) {
					if (method.isAnnotationPresent(MyRequestMapping.class)) {
						MyRequestMapping mrm = method.getAnnotation(MyRequestMapping.class);
						String childUrl = mrm.value();
						String url = classUrl + childUrl;
						methods.put(url, method);
					} else {
						continue;
					}
				}
			} else {
				continue;
			}
		}
	}

	public void doAutowired() {
		for (Map.Entry<String, Object> entry : beans.entrySet()) {
			Object instance = entry.getValue();
			Class<? extends Object> clazz = instance.getClass();
			if (clazz.isAnnotationPresent(MyController.class)) {
				Field[] fields = clazz.getDeclaredFields();
				for (Field f : fields) {
					if (f.isAnnotationPresent(MyAutowired.class)) {
						MyAutowired aw = f.getAnnotation(MyAutowired.class);
						String key = aw.value();
						Object autoWiredBean = beans.get(key);
						f.setAccessible(true);
						try {
							f.set(instance, autoWiredBean);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					} else {
						continue;
					}
				}
			} else {
				continue;
			}
		}
	}

	public void doInstance() {
		for (String className : classNames) {
			try {
				String cn = className.replace(".class", "");
				Class<?> clazz = Class.forName(cn);
				if (clazz.isAnnotationPresent(MyController.class)) {
					Object instance = clazz.newInstance();
					// map.put(K,V)
					MyRequestMapping anno = clazz.getAnnotation(MyRequestMapping.class);
					String key = anno.value();
					beans.put(key, instance);
				} else if (clazz.isAnnotationPresent(MyService.class)) {
					Object instance = clazz.newInstance();
					// map.put(K,V)
					MyService anno = clazz.getAnnotation(MyService.class);
					String key = anno.value();
					beans.put(key, instance);
				} else {
					continue;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	// 扫描包mvc.xml-->basePackage="xx"
	public void doScan(String basePackage) {
		// basePackage = com.szh
		// 扫描编译好的所有类路径
		URL url = this.getClass().getClassLoader().getResource("/" + basePackage.replaceAll("\\.", "/"));
		String fileStr = url.getFile(); // com/szh文件夾
		File file = new File(fileStr);
		String[] fileStrs = file.list(); // com/szh下所有class文件和文件夾
		for (String path : fileStrs) {
			File f = new File(fileStr + path);
			if (f.isDirectory()) {
				doScan(basePackage + "." + path);
			} else {
				// com.szh.xx.class
				classNames.add(basePackage + "." + f.getName());
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 获取请求路径url
		String uri = req.getRequestURI(); //ip:port/mymvc/ctr1/query
		String contextPath = req.getContextPath(); 
		String url = uri.replace(contextPath, ""); // /ctr1/query
		// method.invoke(obj, args); 反射调用方法
		// TestController tc = (TestController) beans.get("/" + url.split("/")[1]);
		Object bean = beans.get("/" + url.split("/")[1]);
		Method method = methods.get(url);
		Object args[] = handle(req, resp, method);
		try {
			method.invoke(bean, args);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取请求参数
	 * @param req
	 * @param resp
	 * @param method
	 * @return
	 */
	private Object[] handle(HttpServletRequest req, HttpServletResponse resp, Method method) {
		// 拿到方法参数
		Class<?>[] parameterTypes = method.getParameterTypes();
		// 根据参数个数,new一个参数数组,将方法里所有参数赋值进args
		Object[] args = new Object[parameterTypes.length];
		int args_i = 0;
		int index = 0;
		for (Class<?> clazz : parameterTypes) {
			if (ServletRequest.class.isAssignableFrom(clazz)) {
				args[args_i++] = req;
			}
			if (ServletResponse.class.isAssignableFrom(clazz)) {
				args[args_i++] = resp;
			}
			// 从0-3判断有没有RequestParam注解,很明显0,1不是
			// 2和3有RequestParam注解,需要解析
			Annotation[] paramAnnos = method.getParameterAnnotations()[index];
			if (paramAnnos.length > 0) {
				for (Annotation anno : paramAnnos) {
					if (MyRequestParam.class.isAssignableFrom(anno.getClass())) {
						MyRequestParam mrp = (MyRequestParam) anno;
						// 找到注解里的name和age
						args[args_i++] = req.getParameter(mrp.value());
					}
				}
			}
			index++;
		}
		return args;
	}
}
