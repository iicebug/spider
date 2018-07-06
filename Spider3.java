package test;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static thinjavatest.MyPrint.*;

/**
下载网站图片的简单爬虫程序；使用同步队列的方式实现
一、步骤如下:
	解析图片网站网页的HTML内容，用正则表达式匹配所有的img标签；
	从img标签中提取图片的具体链接；再和域名拼接成完整的图片的url地址；

	根据每一个url地址，建立URL对象，调用openStream方法，获得图片内容的输入流；
	然后创建一个输出流，即保存图片到磁盘上。

	将每个线程都提交到一个线程池；即可完成所有的下载。

二、存在的问题：
	还未实现自动收集多个页面的url地址，只能手动传入每个页面的地址。

	不同的网站的图片地址的解析可能会出错，正则表达式可能需要随之修改，视网站的HTML代码具体情况而定。

*@author zkj
*/

public class Spider3
{
	public static void main(String[] args) throws Exception
	{
		println("================Spider程序启动!=================");
		
		String downloadDir = "G:/picture" ;
		String pageUrl = "http://pic.netbian.com" ;
		
		UrlQueue pageUrlQueue = new UrlQueue();
		UrlQueue htmlQueue =    new UrlQueue();
		UrlQueue imgTagQueue =  new UrlQueue();
		UrlQueue finalUrlQueue =new UrlQueue();

		ExecutorService exe = Executors.newCachedThreadPool();
		exe.execute(new GetPageUrl (pageUrlQueue));
		exe.execute(new GetHtml    (pageUrlQueue , htmlQueue));
		exe.execute(new GetImgTag  (htmlQueue , imgTagQueue));
		exe.execute(new GetFinalUrl(imgTagQueue , finalUrlQueue , pageUrl));
		exe.execute(new Download   (finalUrlQueue , downloadDir));
		
		//TimeUnit.SECONDS.sleep(5);
		//exe.shutdownNow();//立即中断所有线程
	}
}

class UrlQueue extends LinkedBlockingQueue<String>{}

class GetPageUrl implements Runnable
{
	private UrlQueue pageUrlQueue;
	
	public GetPageUrl(UrlQueue pageUrlQueue)
	{
		this.pageUrlQueue = pageUrlQueue ;
	}

	public void run()
	{
		try
		{
			for (int i = 2 ; i < 3 ; i++)
			{
				String pageUrl = "http://pic.netbian.com/4kmingxing/index_"+ i +".html";
				pageUrlQueue.put(pageUrl);
			}
		}
		catch (InterruptedException e)
		{
			println("GetPageUrl interrupted.....");
		}
		println("\n-------GetPageUrl Has Done--------\n");	
	}
}

class GetHtml implements Runnable
{
	private UrlQueue pageUrlQueue;
	private UrlQueue htmlQueue;

	public GetHtml(UrlQueue pageUrlQueue ,UrlQueue htmlQueue)
	{
		this.pageUrlQueue = pageUrlQueue ;
		this.htmlQueue = htmlQueue ;
	}

	public void run()
	{
		try
		{
			while (!Thread.interrupted())
			{
				String pageUrl = pageUrlQueue.take();
				URL url = new URL(pageUrl);
				//BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
				conn.setRequestProperty("User-Agent", "Mozilla/31.0 (compatible; MSIE 10.0; Windows NT; DigExt)");  
				InputStreamReader isr = new InputStreamReader(conn.getInputStream());  
				BufferedReader br = new BufferedReader(isr);
				StringBuilder sb = new StringBuilder();
				String line = "";
				while ((line = br.readLine()) != null)
				{
					sb.append(line);
				}
				htmlQueue.put(sb.toString());
			}			
		}
		catch (InterruptedException e)
		{
			println("GetHtml interrupted.....");
		}
		catch (MalformedURLException e)
		{
			println("解析URL出错.....");
		}
		catch (IOException e)
		{
			println("获取网页数据出错.....");
		}
		println("-------GetHtml off--------");	
	}
}

class GetImgTag implements Runnable 
{
	private UrlQueue imgTagQueue;
	private UrlQueue htmlQueue;

	public GetImgTag(UrlQueue htmlQueue ,UrlQueue imgTagQueue)
	{
		this.imgTagQueue = imgTagQueue ;
		this.htmlQueue = htmlQueue ;
	}

	public void run()
	{
		try
		{
			while (!Thread.interrupted())
			{
				String htmlText = htmlQueue.take();
				String regex = "(i?)<img.*? src=\"?(.*?\\.(jpg|gif|bmp|bnp|png))\".*? />";
				Pattern pa = Pattern.compile(regex, Pattern.DOTALL);
				Matcher ma = pa.matcher(htmlText);
				while (ma.find())
				{
					imgTagQueue.put(ma.group());
				}
			}
			
		}
		catch (InterruptedException e)
		{
			println("GetImgTag interrupted.....");
		}
		println("-------GetImgTag off--------");
	}
}

class GetFinalUrl implements Runnable
{
	private UrlQueue imgTagQueue;
	private UrlQueue finalUrlQueue;
	private String pageUrl ;

	public GetFinalUrl(UrlQueue imgTagQueue , UrlQueue finalUrlQueue , String pageUrl)
	{
		this.imgTagQueue = imgTagQueue ;
		this.finalUrlQueue = finalUrlQueue ;
		this.pageUrl = pageUrl;
	}
	
	public void run()
	{
		try
		{
			while (!Thread.interrupted())
			{
				String imgTag = imgTagQueue.take();
				int first = imgTag.indexOf("\"");
				String sub = imgTag.substring(first+1);
				int second = sub.indexOf("\"");
				String finalUrl = sub.substring(0,second);
				if (finalUrl.startsWith("http"))
				{
					finalUrlQueue.put(finalUrl);
				}
				else
				{
					//截取初始化地址的首页域名，即http://www.xxxxx.com,此步骤根据具体的网页的图片地址情况来决定如何做。最后得到了最终的图片的地址。
					String dname = pageUrl.substring(0,pageUrl.indexOf("com")+3);
					finalUrlQueue.put(dname+finalUrl);
				}				
			}
		}
		catch (InterruptedException e)
		{
			println("GetFinalUrl interrupted.....");
		}
		println("-------GetFinalUrl off--------");
	}
}


class Download implements Runnable
{
	private UrlQueue finalUrlQueue;	
	private final int BUFF_LEN = 1024;
	private String downloadDir ;
	private InputStream is ;
    private RandomAccessFile raf ;
	private int counter = 1;
	
	public Download(UrlQueue finalUrlQueue , String downloadDir)
	{
		this.finalUrlQueue = finalUrlQueue;
		this.downloadDir = downloadDir;
	}

	public void run()
	{
		try
		{
			while (!Thread.interrupted())
			{
				String finalUrl = finalUrlQueue.take();
				URL url = new URL(finalUrl);
				is = url.openStream();					
				//获得图片在URL中的路径；
				String urlFileName =url.getFile();
				//截取路径中的图片名称部分，获得最终的图片名称
				String fileName = urlFileName.substring(urlFileName.lastIndexOf("/"));
				//获得图片在本地保存的路径；
				String downloadPath = downloadDir + fileName;
				raf = new RandomAccessFile(downloadPath , "rw");
				System.out.print("Picture " + counter++ +"\tDownloading......");	
				byte[] buff = new byte[BUFF_LEN];
				int hasRead = 0;
				while ((hasRead = is.read(buff)) > 0)
				{			
					raf.write(buff, 0, hasRead);
				}
				System.out.println("  [ Download successfully ]");
			}
		}
		catch (InterruptedException e)
		{
			println("DownLoad interrupted.....");
		}
		catch (IOException e)
		{
			System.out.println("======下载出错=====");
		}
		finally
		{
			try
			{
				if (is != null)
				{
					is.close();
				}
				if (raf != null)
				{
					raf.close();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}		
		}
	}
}


