package com.tsolmon.online_teaching_platform.course.infrastructure;

import com.tsolmon.online_teaching_platform.course.domain.CourseCategory;
import com.tsolmon.online_teaching_platform.course.domain.CourseCategoryRepository;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class CourseSeedDataConfig {
    private final CourseCategoryRepository categoryRepository;
    private final CourseSubjectRepository subjectRepository;

    @Bean
    public CommandLineRunner seedCourseCatalog() {
        return args -> {
            Map<String, String[]> seed = new LinkedHashMap<>();
            seed.put("Programming", new String[]{"Java", "Spring Boot", "JavaScript", "React", "TypeScript"});
            seed.put("Data Science", new String[]{"Python", "SQL", "Machine Learning", "Statistics"});
            seed.put("Languages", new String[]{"English", "Korean", "Japanese", "Chinese"});
            seed.put("School Subjects", new String[]{"Math", "Physics", "Chemistry", "Biology"});

            for (Map.Entry<String, String[]> entry : seed.entrySet()) {
                CourseCategory category = categoryRepository.findByNameIgnoreCase(entry.getKey())
                        .orElseGet(() -> {
                            CourseCategory c = new CourseCategory();
                            c.setName(entry.getKey());
                            c.setDescription(entry.getKey() + " category");
                            return categoryRepository.save(c);
                        });

                for (String subjectName : entry.getValue()) {
                    if (subjectRepository.findByNameIgnoreCase(subjectName).isPresent()) {
                        continue;
                    }
                    CourseSubject subject = new CourseSubject();
                    subject.setName(subjectName);
                    subject.setDescription(subjectName + " tutoring");
                    subject.setCategory(category);
                    subjectRepository.save(subject);
                }
            }
        };
    }
}

