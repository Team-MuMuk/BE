package com.mumuk.domain.user.converter;

import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.entity.UserRecipe;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class MypageConverter {

    public static UserResponse.ProfileInfoDTO toProfileInfoDTO(User user) {
        return new UserResponse.ProfileInfoDTO(
                user.getName(),
                user.getNickName(),
                user.getProfileImage(),
                user.getStatusMessage()
        );
    }

    public static UserResponse.LikedRecipeDTO toLikedRecipeDTO(UserRecipe userRecipe) {

        Recipe recipe = userRecipe.getRecipe();
        boolean liked = userRecipe.getLiked();
        return UserResponse.LikedRecipeDTO.builder()
                .recipeId(recipe.getId())
                .recipeName(recipe.getTitle())
                .recipeImage(recipe.getRecipeImage())
                .liked(liked)
                .build();

    }
    public static UserResponse.LikedRecipeListDTO toLikedRecipeListDTO(Page<UserRecipe> likedUserRecipes) {
        List<UserResponse.LikedRecipeDTO> likedRecipeDTOList = likedUserRecipes.stream()
                .map(MypageConverter::toLikedRecipeDTO)
                .collect(Collectors.toList());

        Long userId = null;
        if (!likedUserRecipes.isEmpty()) {
            userId = likedUserRecipes.getContent().get(0).getUser().getId();
        }

        return UserResponse.LikedRecipeListDTO.builder()
                .userId(userId)
                .likedRecipes(likedRecipeDTOList)
                .currentPage(likedUserRecipes.getNumber())
                .totalPages(likedUserRecipes.getTotalPages())
                .totalElements(likedUserRecipes.getTotalElements())
                .pageSize(likedUserRecipes.getSize())
                .hasNext(likedUserRecipes.hasNext())
                .build();
    }
}
