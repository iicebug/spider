package test;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
������վͼƬ�ļ��������
һ����������:
	����ͼƬ��վ��ҳ��HTML���ݣ���������ʽƥ�����е�img��ǩ��
	��img��ǩ����ȡͼƬ�ľ������ӣ��ٺ�����ƴ�ӳ�������ͼƬ��url��ַ��

	����ÿһ��url��ַ������URL���󣬵���openStream���������ͼƬ���ݵ���������
	Ȼ�󴴽�һ���������������ͼƬ�������ϡ�

	�����롢������Ķ�дͨ���߳�����ɣ�

	��ÿ���̶߳��ύ��һ���̳߳أ�����������е����ء�

�������ڵ����⣺
	��δʵ���Զ��ռ����ҳ���url��ַ��ֻ���ֶ�����ÿ��ҳ��ĵ�ַ��

	��ͬ����վ��ͼƬ��ַ�Ľ������ܻ����������ʽ������Ҫ��֮�޸ģ�����վ��HTML����������������

*@author
*/

public class Spider2
{
	public static void main(String[] args) throws Exception
	{
		System.out.println("=========���س�������!==========");
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
		//ƥ�䵽img��ǩ��
		regex = "(i?)<img.*? src=\"?(.*?\\.(jpg|gif|bmp|bnp|png))\".*? />";
		Pattern pa = Pattern.compile(regex, Pattern.DOTALL);
		Matcher ma = pa.matcher(htmlText);
		while (ma.find())
		{
			linkList.add(ma.group());
		}
		//��img��ǩ�н�ȡͼƬ�ľ����ַ��
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
				//��ȡ��ʼ����ַ����ҳ��������http://www.xxxxx.com,�˲�����ݾ������ҳ��ͼƬ��ַ�������������������õ������յ�ͼƬ�ĵ�ַ��
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
			System.out.println("ͼƬ����������...........");	
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
			System.out.println("======���س����߳���ֹ=====");
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
			//pool.shutdown();	���ܹر��̳߳أ�����ֻ������һ���̣߳�	
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
				//���ͼƬ��URL�е�·����
				String urlFileName =url.getFile();
				//��ȡ·���е�ͼƬ���Ʋ��֣�������յ�ͼƬ����
				String fileName = urlFileName.substring(urlFileName.lastIndexOf("/"));
				//���ͼƬ�ڱ��ر����·����
				String downloadFile = downloadDir + fileName;
				//�����ļ����������
				raf = new RandomAccessFile(downloadFile , "rw");
				//�ύ���ص��̸߳��̳߳أ�
				DownloadPool.submitThread(new DownloadThread(is,raf));		
			}
			catch (Exception e)
			{
				System.out.println("***********��ͼƬ�޷�����**********");
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