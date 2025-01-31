package kh.edu.social_members.service;

import kh.edu.social_members.dto.Member;
import kh.edu.social_members.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl implements MemberService {
    @Autowired
    MemberMapper memberMapper;


    // BCryptPasswordEncoder 로 비밀번호 암호화 처리해서 저장
    @Override
    public void insertMember(Member member) {
        //   dty username = set (kakao.username);
        memberMapper.insertMember(member);
    }
}
