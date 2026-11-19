package com.diginationmea.blogimo.post;

import com.diginationmea.blogimo.category.Category;
import com.diginationmea.blogimo.category.CategoryRepository;
import com.diginationmea.blogimo.common.exception.ResourceNotFoundException;
import com.diginationmea.blogimo.post.dto.PostRequest;
import com.diginationmea.blogimo.post.dto.PostResponse;
import com.diginationmea.blogimo.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository posts;
    private final CategoryRepository categories;
    private final UserRepository author;

    public PostService(PostRepository posts, CategoryRepository categories, UserRepository author) {
        this.posts = posts;
        this.categories = categories;
        this.author = author;
    }

    public Page<PostResponse> list(Pageable pageable) {
        return posts.findAll(pageable).map(PostService::toResponse);
    }

    public PostResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public PostResponse create(PostRequest req) {
        Post post = new Post();
        post.setAuthor(author.findById(req.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.authorId())));
        return toResponse(posts.save(apply(post, req)));
    }

    @Transactional
    public PostResponse update(Long id, PostRequest req) {
        return toResponse(apply(find(id), req));
    }

    @Transactional
    public void delete(Long id) {
        posts.delete(find(id));
    }

    private Post find(Long id) {
        return posts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    private Post apply(Post post, PostRequest req) {
        Category category = categories.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        post.setTitle(req.title());
        post.setContent(req.content());
        post.setCategory(category);
        return post;
    }

    private static PostResponse toResponse(Post p) {
        return new PostResponse(p.getId(), p.getTitle(), p.getContent(),
                p.getCategory().getId(), p.getAuthor().getId(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
