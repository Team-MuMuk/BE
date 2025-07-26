package com.mumuk.domain.user.converter;

import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
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

    public static UserRecipeResponse.RecentRecipeDTO toLikedRecipeDTO(UserRecipe userRecipe) {

        Recipe recipe = userRecipe.getRecipe();
        boolean liked = userRecipe.getLiked();
        return UserRecipeResponse.RecentRecipeDTO.builder()
                .recipeId(recipe.getId())
                .name(recipe.getTitle())
                .imageUrl(recipe.getRecipeImage())
                .liked(liked)
                .build();

    }
    public static UserRecipeResponse.LikedRecipeListDTO toLikedRecipeListDTO(Long userId, Page<UserRecipe> likedUserRecipes) {
        List<UserRecipeResponse.RecentRecipeDTO> likedRecipeDTOList = likedUserRecipes.stream()
                .map(MypageConverter::toLikedRecipeDTO)
                .collect(Collectors.toList());

        return UserRecipeResponse.LikedRecipeListDTO.builder()
                .userId(userId)
                .likedRecipes(likedRecipeDTOList)
                .currentPage(likedUserRecipes.getNumber()+1)
                .totalPages(likedUserRecipes.getTotalPages())
                .totalElements(likedUserRecipes.getTotalElements())
                .pageSize(likedUserRecipes.getSize())
                .hasNext(likedUserRecipes.hasNext())
                .build();
    }
}
