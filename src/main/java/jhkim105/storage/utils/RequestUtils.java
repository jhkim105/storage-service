package jhkim105.storage.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestUtils {


  public static String extractPath(HttpServletRequest request) {
    String path = (String)request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    String matchPattern = (String)request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE); //
    return new AntPathMatcher().extractPathWithinPattern(matchPattern, path);
  }
}
