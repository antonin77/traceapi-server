package org.usf.trace.api.server;

import static java.util.Objects.isNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.usf.trace.api.server.Utils.requireSingle;
import static org.usf.traceapi.core.Session.nextId;

import java.time.Instant;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.traceapi.core.ApiRequest;
import org.usf.traceapi.core.ApiSession;
import org.usf.traceapi.core.ApplicationInfo;
import org.usf.traceapi.core.MainSession;
import org.usf.traceapi.core.Session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ApiController {
	
    private final RequestDao dao;
    private final SessionQueueService queueService;

    @PutMapping("session")
    public ResponseEntity<Void> saveSession(HttpServletRequest hsr,@RequestBody Session[] sessions) {
        for(Session s : sessions) {
            if(isNull(s.getId())) {
                if(s instanceof MainSession) {
                    s.setId(nextId()); // safe id set for web collectors
                    updateRemoteAddress(hsr,(MainSession) s);
                }
                else if(s instanceof ApiSession) {
                    log.warn("ApiSesstion id is null : {}", s);
                }
            }
        }
        queueService.add(sessions);
        return accepted().build();
    }
    public void  updateRemoteAddress(HttpServletRequest hsr,  @RequestBody MainSession req) {
    	if(isNull(req.getApplication())) { //set IP address for WABAPP trace
    		req.setApplication(new ApplicationInfo(null, null, hsr.getRemoteAddr(), null, null, null));
    	}
    	else if(isNull(req.getApplication().getAddress())) {
    		req.setApplication(req.getApplication().withAddress(hsr.getRemoteAddr()));
    	}
    }

    @GetMapping("incoming/request")
    public List<ApiSession> getIncomingRequestByCriteria(
    		@RequestParam(defaultValue = "true", name = "lazy") boolean lazy, 
    		@RequestParam(required = false, name = "id") String[] id,
    		@RequestParam(required = false, name = "name") String[] name,
    		@RequestParam(required = false, name = "env") String[] env,
    		@RequestParam(required = false, name = "port") String[] port,
    		@RequestParam(required = false, name = "start") Instant start,
    		@RequestParam(required = false, name = "end") Instant end ){ // without tree
        FilterCriteria fc = new FilterCriteria(id,name,env,port,null,start,end);
        return dao.getIncomingRequestByCriteria(lazy,fc);
    }

    @GetMapping("incoming/request/{id}")
    public Session getIncomingRequestById(@PathVariable String id) { // without tree
        return requireSingle(dao.getIncomingRequestById(true, id));
    }

    @GetMapping("main/request")
    public List<MainSession> getMainRequestByCriteria(
            @RequestParam(defaultValue = "true", name = "lazy") boolean lazy,
            @RequestParam(required = false, name = "id") String[] id,
            @RequestParam(required = false, name = "env") String[] env,
            @RequestParam(required = false, name = "launchmode") String[] launchMode,
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end ) {

        FilterCriteria fc = new FilterCriteria(id,null,env,null,launchMode,start,end);
        return dao.getMainRequestByCriteria(lazy,fc);
    }

    @GetMapping("main/request/{id}")
    public MainSession getMainRequestById(@PathVariable String id) { // without tree
        return requireSingle(dao.getMainRequestById(true, id));
    }

    @GetMapping("incoming/request/{id}/out")
    public ApiRequest getOutcomingRequestById(@PathVariable String id) {
        return dao.getOutcomingRequestById(id);
    }

    @GetMapping("incoming/request/{id}/tree") //LATER
    public Session getIncomingRequestTreeById(@PathVariable String id) {
        return requireSingle(dao.getIncomingRequestById(true, id)); //change query
    }
    
}


