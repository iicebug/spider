package test;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
下载网站图片的简单爬虫程序；
一、步骤如下:
	解析图片网站网页的HTML内容，用正则表达式匹配所有的img标签；
	从img标签中提取图片的具体链接；再和域名拼接成完整的图片的url地址；

	根据每一个url地址，建立URL对象，调用openStream方法，获得图片内容的输入流；
	然后创建一个输出流，即保存图片到磁盘上。

	对输入、输出流的读写通过线程来完成；

	将每个线程都提交到一个线程池；即可完成所有的下载。

二、存在的问题：
	还未实现自动收集多个页面的url地址，只能手动传入每个页面的地址。

	不同的网站的图片地址的解析可能会出错，正则表达式可能需要随之修改，视网站的HTML代码具体情况而定。

*@author
*/

public class Spider2
{
	public static void main(String[] args) throws Exception
	{
		System.out.println("=========下载程序启动!==========");
		for (int i = 2; i < 50; i++)
		{
			InitDownload.init("http://pic.netbian.com/4kmeinv/index_"+i+".html" , "G:\\picture", 5);
		}
		
	}
}

class GetPictureLink
{
	public ArrayList<String> urlList = null;
	public String domainName = null;
	public String htmlText = null;
	public URL url = null;
	public BufferedReader br = null;
	public GetPictureLink(String domainName) throws Exception
	{
		this.domainName = domainName ;
		urlList = new ArrayList<String>();

	}
	public void getHTML() throws Exception
	{
		url = new URL(domainName);
		br = new BufferedReader(new InputStreamReader(url.openStream()));
		StringBuffer sb = new StringBuffer();
		String line = "";
		while ((line = br.readLine()) != null)
		{

			sb.append(line);
		}
		htmlText = sb.toString();
	
	}
	public void getLink() 
	{
		String regex;
		ArrayList<String> linkList = new ArrayList<String>();
		//匹配到img标签；
		regex = "(i?)<img.*? src=\"?(.*?\\.(jpg|gif|bmp|bnp|png))\".*? />";
		Pattern pa = Pattern.compile(regex, Pattern.DOTALL);
		Matcher ma = pa.matcher(htmlText);
		while (ma.find())
		{
			linkList.add(ma.group());
		}
		//从img标签中截取图片的具体地址：
		for (String link : linkList)
		{
			int first = link.indexOf("\"");
			String sub = link.substring(first+1);
			int second = sub.indexOf("\"");
			String finalLink = sub.substring(0,second);
			if (finalLink.startsWith("http"))
			{
				urlList.add(finalLink);
			}
			else
			{
				//截取初始化地址的首页域名，即http://www.xxxxx.com,此步骤根据具体的网页的图片地址情况来决定如何做。最后得到了最终的图片的地址。
				String dname = domainName.substring(0,domainName.indexOf("com")+3);
				urlList.add(dname+finalLink);
			}
			
		}
    }
}

class DownloadThread implements Runnable
{
	private final int BUFF_LEN = 1024;
	private InputStream is ;
	private RandomAccessFile raf ;

	public DownloadThread(InputStream is, RandomAccessFile raf)
	{
		this.is = is;
		this.raf = raf;	
	}
	public void run()
	{
		try
		{
			System.out.println("图片正在下载中...........");	
			byte[] buff = new byte[BUFF_LEN];
			int hasRead = 0;
			while ((hasRead = is.read(buff)) > 0)
			{
		
				raf.write(buff, 0, hasRead);
			}
			System.out.println("Download successfully...........");
		}
		catch (Exception e)
		{
			System.out.println("======下载出错，线程终止=====");
			//Thread.currentThread().stop();
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

class DownloadPool
{
		private static int MAX_THREAD_NUM ;
		private static ExecutorService pool ;
		public DownloadPool( int num )
		{
			this.MAX_THREAD_NUM = num;
			pool = Executors.newFixedThreadPool(MAX_THREAD_NUM);
		}
		public static void submitThread(DownloadThread downThread)
	    {	
			pool.submit(downThread);
			//pool.shutdown();	不能关闭线程池，否则只能启动一条线程；	
		}

}

class Download
{
	String downloadDir;
	InputStream is;
	RandomAccessFile raf;
	public void setDownloadDir(String dir)
	{
		this.downloadDir = dir;
	}
	public void downStart(List<String> urlList)
	{
		for (String link : urlList)
		{
			try
			{
				URL url = new URL(link);
				is = url.openStream();					
				//获得图片在URL中的路径；
				String urlFileName =url.getFile();
				//截取路径中的图片名称部分，获得最终的图片名称
				String fileName = urlFileName.substring(urlFileName.lastIndexOf("/"));
				//获得图片在本地保存的路径；
				String downloadFile = downloadDir + fileName;
				//创建文件输出流对象；
				raf = new RandomAccessFile(downloadFile , "rw");
				//提交下载的线程给线程池；
				DownloadPool.submitThread(new DownloadThread(is,raf));		
			}
			catch (Exception e)
			{
				System.out.println("***********该图片无法下载**********");
				continue;
			}
	
		}
	
	}
}

class InitDownload
{
	public static void init(String initUrl, String dir, int threadNum) throws Exception
	{
		Download downPicture = new Download();
		downPicture.setDownloadDir(dir);
		new DownloadPool(threadNum);	
		GetPictureLink gpl = new GetPictureLink(initUrl);
		gpl.getHTML();
		gpl.getLink();
		downPicture.downStart(gpl.urlList);
	}
}