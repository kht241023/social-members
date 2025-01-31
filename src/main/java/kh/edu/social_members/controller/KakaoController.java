package kh.edu.social_members.controller;

import kh.edu.social_members.service.MemberServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
public class KakaoController {
    // ${변수이름} application.properties 나 config.properties 에 작성한 변수이름 가져오기
    // 변수이름에 해당하는 값을 불러오기
    @Value("${kakao.client-id}") // = ${REST_API_KEY}
    private String kakaoClientId;

    @Autowired
    private MemberServiceImpl memberService;
    /*
    config.properties 에서  kakao.redirect-uri 직접적으로 가져올 수 있음
    private String  kakao.redirect-uri=http://localhost:8080/oauth/kakao/callback
    java-spring  자체에서 보안을 가장 중요하게 생각하기 때문에
    아이디, 비밀번호와 같은 중요한 정보는 properties 파일로 나누어서
    @Value 값으로 호출해서 사용할 수 있도록 분류해주는 것이 바람직함
     */
    @Value("${kakao.redirect-uri}")  // = ${REDIRECT_URI}"
    private String redirectUri;

    @Value("${kakao.client-secret}")
    private String kakaoClientSecret;

    @GetMapping("/auth/kakao/callback") // RequestMapping + GetMapping = /oauth/kakao/login
    public ResponseEntity<?> getKakaoLoginUrl() { // ResponseEntity<?> 작성을 안해도 됨 현재 제대로 진행되고 있는지 상태 확인일 뿐
        // 카카오톡 개발 문서 에서 카카오로그인 > 예제 > 요청에 작성된 주소를 그대로 가져온 상태
        // String url = "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=${REST_API_KEY}&redirect_uri=${REDIRECT_URI}";
        String url = "https://kauth.kakao.com/oauth/authorize?response_type=code" +
                "&client_id=" + kakaoClientId + "&redirect_uri=" + redirectUri;
        return ResponseEntity.ok(url);
    }

// kakao.redirect-uri=http://localhost:8080/oauth/kakao/callback

    @GetMapping("/login/oauth2/code/kakao") // /oauth/kakao/callback
    public String handleCallback(@RequestParam String code) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        if (kakaoClientSecret != null) {
            params.add("client_secret", kakaoClientSecret);
        }

        HttpEntity<LinkedMultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        String accessToken = (String) response.getBody().get("access_token");

        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.add("Authorization", "Bearer " + accessToken);

        HttpEntity<String> userRequest = new HttpEntity<>(userHeaders);
        ResponseEntity<Map> userResponse = restTemplate.postForEntity(userInfoUrl, userRequest, Map.class);

        Map userInfo = userResponse.getBody();
        System.out.println("=================[Controller] - user info=================");
        System.out.println(userInfo);
        Map<String, Object> properties = (Map<String, Object>) userInfo.get("properties");

        String nickname = (String) properties.get("nickname"); //현재는 닉네임만 가져오도록 설정한 상태
        String encodedNickname = URLEncoder.encode(nickname, StandardCharsets.UTF_8); // 한글 깨짐 방지

        String profileImg = (String) properties.get("profile_image");


        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        String name = (String) kakaoAccount.get("name");
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8); // 한글 깨짐 방지
        String gender = (String) kakaoAccount.get("gender");


        return "redirect:/signup?nickname=" + encodedNickname + "&email=" + email +
                "&name=" + encodedName + "&gender=" + gender
                + "&profileImg=" + profileImg;

        /* signup.html 에서 회원가입란을 작성하지 않고,
        카카오 로그인 클릭 후 바로 DB에 저장하는 방식

        예전에는 아래와 같은 방식을 주로 사용
        로그인하는 회사별로 사용하는 json 형식을 모두 파악
        service 에서 개발자가 처리하는 로직에서 문제가 발생
        DB에 값이 제대로 넘어오지 않는 경우 존재 -> 소셜 변수명 변경, JSON 형식 변경했을 때
        memberService.insertMember(nickname, name, email, gender);
        return "DB에 저장완료";
        */

    }

}
