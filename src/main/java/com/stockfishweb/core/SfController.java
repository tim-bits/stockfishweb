package com.stockfishweb.core;

import com.stockfishweb.core.engine.enums.Query;
import com.stockfishweb.model.BestMoveEval;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SfController {

    private static final Log logger = LogFactory.getLog(SfController.class);
    @Autowired
    private SfService sfService;

    /**
     * Migrating away from CompletableFuture in favor of Spring @Async
     * @param query
     * @return
     */
    @PostMapping(
            path = "/",
            consumes="application/json", produces="application/json")
    @ResponseBody
    public BestMoveEval postBestMoveEval(@RequestBody Query query, HttpServletRequest request) {
        logger.debug(request.getRemoteAddr());
        return sfService.getBestMoveEvalSync(query);
    }

    /**
     * Not to be used
     *
     * @return
     */
    @GetMapping(
            path = "/",
            produces="application/json")
    @ResponseBody
    public Map<String, String> getBestMoveEval() {
        Map<String, String> map = new HashMap<>();
        map.put("error", "POST must be used instead of GET");
        return map;
    }
}
