package test;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;



public class Spider
{
	public static void main(String[] args) 
	{
		System.out.println("=========下载程序启动!==========");
		InitDownload.init("http://pics.sc.chinaz.com/files/pic/pic9/201804/zzpic11216.jpg" , "G:\\picture", 10);


	}
}

class GetUrl
{
	private String url = null;
	public GetUrl(String url)
	{
		this.url = url ;
	}
	public String getUrl()
	{
		return url;
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
			e.printStackTrace();
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
		public static void download(DownloadThread downThread)
	    {	

			pool.submit(downThread);

			pool.shutdown();


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
	public void downStart(String newurl)
	{
		try
		{
			URL url = new URL(newurl);
			is = url.openStream();
			//获得文件在URL中的路径；
			String urlFileName =url.getFile();
			//截取路径中的文件名称部分，获得最终的文件名称
			String fileName = urlFileName.substring(urlFileName.lastIndexOf("/"));
			String downloadFile = downloadDir + fileName;

			raf = new RandomAccessFile(downloadFile , "rw");
			//new DownloadThread(is,raf);
			//DownloadPool dlp = new DownloadPool(10);
			DownloadPool.download(new DownloadThread(is,raf));			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

class InitDownload
{
	public static void init(String url, String dir, int threadNum)
	{
		GetUrl gurl = new GetUrl(url);
		Download down = new Download();
		
		new DownloadPool(threadNum);
		
		down.setDownloadDir(dir);
		down.downStart(url);

	}
}