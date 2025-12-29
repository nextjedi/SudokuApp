import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { blogPosts, BlogPost } from "../utils/blogData";

interface BlogScreenProps {
  onBack: () => void;
  onPostPress: (postId: string) => void;
}

export const BlogScreen: React.FC<BlogScreenProps> = ({
  onBack,
  onPostPress,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<
    "all" | "rules" | "benefits" | "tips"
  >("all");

  const filteredPosts =
    selectedCategory === "all"
      ? blogPosts
      : blogPosts.filter((post) => post.category === selectedCategory);

  const getCategoryEmoji = (category: BlogPost["category"]): string => {
    switch (category) {
      case "rules":
        return "📚";
      case "benefits":
        return "🧠";
      case "tips":
        return "💡";
      default:
        return "📝";
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={onBack} style={styles.backButton}>
          <Text style={styles.backText}>← Back</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Sudoku Blog</Text>
        <View style={{ width: 60 }} />
      </View>

      <Text style={styles.subtitle}>
        Learn the rules, discover the benefits, and master advanced strategies
      </Text>

      <View style={styles.categoryFilter}>
        <TouchableOpacity
          style={[
            styles.categoryChip,
            selectedCategory === "all" && styles.categoryChipActive,
          ]}
          onPress={() => {
            setSelectedCategory("all");
          }}
        >
          <Text
            style={[
              styles.categoryText,
              selectedCategory === "all" && styles.categoryTextActive,
            ]}
          >
            All Posts
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[
            styles.categoryChip,
            selectedCategory === "rules" && styles.categoryChipActive,
          ]}
          onPress={() => {
            setSelectedCategory("rules");
          }}
        >
          <Text
            style={[
              styles.categoryText,
              selectedCategory === "rules" && styles.categoryTextActive,
            ]}
          >
            📚 Rules
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[
            styles.categoryChip,
            selectedCategory === "benefits" && styles.categoryChipActive,
          ]}
          onPress={() => {
            setSelectedCategory("benefits");
          }}
        >
          <Text
            style={[
              styles.categoryText,
              selectedCategory === "benefits" && styles.categoryTextActive,
            ]}
          >
            🧠 Benefits
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[
            styles.categoryChip,
            selectedCategory === "tips" && styles.categoryChipActive,
          ]}
          onPress={() => {
            setSelectedCategory("tips");
          }}
        >
          <Text
            style={[
              styles.categoryText,
              selectedCategory === "tips" && styles.categoryTextActive,
            ]}
          >
            💡 Tips
          </Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={filteredPosts}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.postCard}
            onPress={() => {
              onPostPress(item.id);
            }}
            activeOpacity={0.7}
          >
            <View style={styles.postCategory}>
              <Text style={styles.categoryEmoji}>
                {getCategoryEmoji(item.category)}
              </Text>
              <Text style={styles.categoryLabel}>
                {item.category.toUpperCase()}
              </Text>
            </View>
            <Text style={styles.postTitle}>{item.title}</Text>
            <Text style={styles.postExcerpt}>{item.excerpt}</Text>
            <View style={styles.postMeta}>
              <Text style={styles.postDate}>📅 {item.date}</Text>
              <Text style={styles.postReadTime}>⏱️ {item.readTime}</Text>
            </View>
          </TouchableOpacity>
        )}
        contentContainerStyle={styles.postsList}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F8F9FA",
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 24,
    paddingVertical: 16,
  },
  backButton: {
    padding: 8,
  },
  backText: {
    fontSize: 16,
    fontWeight: "600",
    color: "#4A90E2",
  },
  title: {
    fontSize: 24,
    fontWeight: "700",
    color: "#2C3E50",
  },
  subtitle: {
    fontSize: 14,
    color: "#7F8C8D",
    textAlign: "center",
    paddingHorizontal: 24,
    marginBottom: 16,
  },
  categoryFilter: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    paddingHorizontal: 24,
    marginBottom: 16,
  },
  categoryChip: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    borderWidth: 2,
    borderColor: "#E9ECEF",
    backgroundColor: "#FFFFFF",
  },
  categoryChipActive: {
    backgroundColor: "#4A90E2",
    borderColor: "#4A90E2",
  },
  categoryText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#7F8C8D",
  },
  categoryTextActive: {
    color: "#FFFFFF",
  },
  postsList: {
    paddingHorizontal: 24,
    paddingBottom: 24,
  },
  postCard: {
    backgroundColor: "#FFFFFF",
    padding: 20,
    borderRadius: 12,
    marginBottom: 16,
    elevation: 2,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  postCategory: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    backgroundColor: "#E3F2FD",
    paddingVertical: 4,
    paddingHorizontal: 12,
    borderRadius: 12,
    alignSelf: "flex-start",
    marginBottom: 12,
  },
  categoryEmoji: {
    fontSize: 14,
  },
  categoryLabel: {
    fontSize: 12,
    fontWeight: "600",
    color: "#4A90E2",
  },
  postTitle: {
    fontSize: 18,
    fontWeight: "700",
    color: "#2C3E50",
    marginBottom: 8,
  },
  postExcerpt: {
    fontSize: 14,
    color: "#7F8C8D",
    lineHeight: 20,
    marginBottom: 12,
  },
  postMeta: {
    flexDirection: "row",
    gap: 16,
  },
  postDate: {
    fontSize: 12,
    color: "#95A5A6",
  },
  postReadTime: {
    fontSize: 12,
    color: "#95A5A6",
  },
});
