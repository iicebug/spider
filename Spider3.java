package test;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static thinjavatest.MyPrint.*;

/**
������վͼƬ�ļ��������ʹ��ͬ�����еķ�ʽʵ��
һ����������:
	����ͼƬ��վ��ҳ��HTML���ݣ���������ʽƥ�����е�img��ǩ��
	��img��ǩ����ȡͼƬ�ľ������ӣ��ٺ�����ƴ�ӳ�������ͼƬ��url��ַ��

	����ÿһ��url��ַ������URL���󣬵���openStream���������ͼƬ���ݵ���������
	Ȼ�󴴽�һ���������������ͼƬ�������ϡ�

	��ÿ���̶߳��ύ��һ���̳߳أ�����������е����ء�

�������ڵ����⣺
	��δʵ���Զ��ռ����ҳ���url��ַ��ֻ���ֶ�����ÿ��ҳ��ĵ�ַ��

	��ͬ����վ��ͼƬ��ַ�Ľ������ܻ����������ʽ������Ҫ��֮�޸ģ�����վ��HTML����������������

*@author zkj
*/

public class Spider3
{
	public static void main(String[] args) throws Exception
	{
		println("================Spider��������!=================");
		
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
		//exe.shutdownNow();//�����ж������߳�
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
			println("����URL����.....");
		}
		catch (IOException e)
		{
			println("��ȡ��ҳ���ݳ���.....");
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
					//��ȡ��ʼ����ַ����ҳ��������http://www.xxxxx.com,�˲�����ݾ������ҳ��ͼƬ��ַ�������������������õ������յ�ͼƬ�ĵ�ַ��
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
				//���ͼƬ��URL�е�·����
				String urlFileName =url.getFile();
				//��ȡ·���е�ͼƬ���Ʋ��֣�������յ�ͼƬ����
				String fileName = urlFileName.substring(urlFileName.lastIndexOf("/"));
				//���ͼƬ�ڱ��ر����·����
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
			System.out.println("======���س���=====");
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


