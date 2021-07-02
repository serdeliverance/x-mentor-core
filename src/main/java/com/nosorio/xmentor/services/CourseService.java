package com.nosorio.xmentor.services;

import com.nosorio.xmentor.Constants;
import com.nosorio.xmentor.dtos.CoursePagination;
import com.nosorio.xmentor.models.Course;
import com.nosorio.xmentor.models.events.CourseCreated;
import com.nosorio.xmentor.repositories.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String topicExchangeName;

    public Course createCourse(Course course){
        rabbitTemplate.convertAndSend(topicExchangeName, "course.creation", new CourseCreated(course, Instant.now().getEpochSecond()));
        return courseRepository.save(course);
    }

    public void enrollCourse(String courseId, String username) throws InterruptedException {
        Course course = this.getCourseById(courseId);
    }

    public CoursePagination getCoursesByQuery(String query, int page){
        Pageable pageable = PageRequest.of(page, Constants.ITEMS_PER_PAGE);
        Page<Course> coursePage = courseRepository.findByTitleIgnoreCaseContainingOrDescriptionIgnoreCaseContaining(query, query, pageable);
        return new CoursePagination(coursePage.getContent(), coursePage.getTotalElements());
    }

    public Course getCourseById(String courseId){
        return courseRepository.findByUuid(UUID.fromString(courseId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find course"));
    }

    public void rateCourse(String courseId){

    }

}
