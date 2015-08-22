package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import retrofit.client.Response;
import retrofit.mime.TypedFile;

@Controller
public class VideoSvcController {

	public static final String DATA_PARAMETER = "data";

	public static final String ID_PARAMETER = "id";

	public static final String RATING_PARAMETER = "rating";
	
	public static final String VIDEO_SVC_PATH = "/video";
	
	public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";
	
	public static final String VIDEO_ITEM_PATH = VIDEO_SVC_PATH + "/{id}";
	
	private static final AtomicLong currentId = new AtomicLong(0L);
	
	private Map<Long,Video> videos = new HashMap<Long, Video>();
		
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videos.values();
	}

	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		checkAndSetId(v);
		v.setDataUrl(getUrlBaseForLocalServer() + VIDEO_SVC_PATH + videos.size() + "/data");		
		videos.put(v.getId(), v);
		return v;
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_ITEM_PATH, method = RequestMethod.GET)
	public @ResponseBody float getVideoRating(
			@PathVariable("id") long id)  {
				
		Video video = videos.get(id);
		
		if (video != null) {
			
			return video.getRating();
			
		} else {
						
			return 0.0f;
		}		
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_ITEM_PATH, method = RequestMethod.POST)
	public @ResponseBody boolean setVideoRating(
			@PathVariable("id") long id,
			@RequestBody float rating)  {
				
		Video video = videos.get(id);
		
		if (video != null) {
			
			video.setRating(rating);
			
			return true;
			
		} else {
						
			return false;
		}		
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable("id") long id,
			@RequestPart(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData,
			HttpServletResponse mResponse)  {
				
		Video video = videos.get(id);
		
		if (video != null) {
			try {
				VideoFileManager vfm = VideoFileManager.get();
				vfm.saveVideoData(video, videoData.getInputStream());
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
			
			return new VideoStatus(VideoState.READY);
			
		} else {
						
			mResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			
			return null;
		}		
	}
		
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
	public @ResponseBody void getData(
			@PathVariable("id") long id,
			HttpServletResponse mResponse)  {
		
		Video video = videos.get(id);
		
		if (video != null) {		
			try {
				VideoFileManager vfm = VideoFileManager.get();
				vfm.copyVideoData(video, mResponse.getOutputStream());
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			
			mResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private String getUrlBaseForLocalServer() {
		
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		
		String base = 
				"http://"+request.getServerName()+((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		
		return base;
	}
	
	private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }
}
