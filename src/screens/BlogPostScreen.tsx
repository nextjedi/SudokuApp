import React from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { blogPosts } from "../utils/blogData";

interface BlogPostScreenProps {
  postId: string;
  onBack: () => void;
}

export const BlogPostScreen: React.FC<BlogPostScreenProps> = ({
  postId,
  onBack,
}) => {
  const post = blogPosts.find((p) => p.id === postId);

  if (!post) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.notFound}>
          <Text style={styles.notFoundTitle}>Post Not Found</Text>
          <Text style={styles.notFoundText}>
            The blog post you're looking for doesn't exist.
          </Text>
          <TouchableOpacity onPress={onBack} style={styles.backButton}>
            <Text style={styles.backButtonText}>Back to Blog</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  const getCategoryEmoji = (category: string): string => {
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

  // Parse markdown-like content
  const renderContent = () => {
    const lines = post.content.split("\n");
    const elements: React.ReactElement[] = [];
    let key = 0;

    lines.forEach((line) => {
      if (line.startsWith("# ")) {
        elements.push(
          <Text key={key++} style={styles.contentH1}>
            {line.substring(2)}
          </Text>,
        );
      } else if (line.startsWith("## ")) {
        elements.push(
          <Text key={key++} style={styles.contentH2}>
            {line.substring(3)}
          </Text>,
        );
      } else if (line.startsWith("### ")) {
        elements.push(
          <Text key={key++} style={styles.contentH3}>
            {line.substring(4)}
          </Text>,
        );
      } else if (line.startsWith("- ")) {
        elements.push(
          <View key={key++} style={styles.listItem}>
            <Text style={styles.bullet}>• </Text>
            <Text style={styles.contentP}>{line.substring(2)}</Text>
          </View>,
        );
      } else if (line.trim() !== "") {
        elements.push(
          <Text key={key++} style={styles.contentP}>
            {line}
          </Text>,
        );
      }
    });

    return elements;
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <TouchableOpacity onPress={onBack} style={styles.headerBackButton}>
          <Text style={styles.headerBackText}>← Back to Blog</Text>
        </TouchableOpacity>

        <View style={styles.postCategoryBadge}>
          <Text style={styles.badgeEmoji}>
            {getCategoryEmoji(post.category)}
          </Text>
          <Text style={styles.badgeText}>{post.category.toUpperCase()}</Text>
        </View>

        <Text style={styles.postTitleMain}>{post.title}</Text>

        <View style={styles.postMetaInfo}>
          <Text style={styles.metaDate}>📅 {post.date}</Text>
          <Text style={styles.metaReadTime}>⏱️ {post.readTime}</Text>
        </View>

        <View style={styles.postBody}>{renderContent()}</View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F8F9FA",
  },
  scrollContent: {
    paddingHorizontal: 24,
    paddingVertical: 16,
  },
  headerBackButton: {
    marginBottom: 20,
  },
  headerBackText: {
    fontSize: 16,
    fontWeight: "600",
    color: "#4A90E2",
  },
  notFound: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: 40,
  },
  notFoundTitle: {
    fontSize: 24,
    fontWeight: "700",
    color: "#2C3E50",
    marginBottom: 12,
  },
  notFoundText: {
    fontSize: 16,
    color: "#7F8C8D",
    textAlign: "center",
    marginBottom: 24,
  },
  backButton: {
    backgroundColor: "#4A90E2",
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  backButtonText: {
    fontSize: 16,
    fontWeight: "600",
    color: "#FFFFFF",
  },
  postCategoryBadge: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#E3F2FD",
    paddingVertical: 6,
    paddingHorizontal: 16,
    borderRadius: 20,
    alignSelf: "flex-start",
    marginBottom: 16,
  },
  badgeEmoji: {
    fontSize: 16,
  },
  badgeText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#4A90E2",
  },
  postTitleMain: {
    fontSize: 28,
    fontWeight: "700",
    color: "#2C3E50",
    lineHeight: 36,
    marginBottom: 16,
  },
  postMetaInfo: {
    flexDirection: "row",
    gap: 20,
    marginBottom: 24,
    paddingBottom: 16,
    borderBottomWidth: 2,
    borderBottomColor: "#E9ECEF",
  },
  metaDate: {
    fontSize: 14,
    color: "#95A5A6",
  },
  metaReadTime: {
    fontSize: 14,
    color: "#95A5A6",
  },
  postBody: {
    marginBottom: 32,
  },
  contentH1: {
    fontSize: 26,
    fontWeight: "700",
    color: "#2C3E50",
    marginTop: 24,
    marginBottom: 12,
  },
  contentH2: {
    fontSize: 22,
    fontWeight: "700",
    color: "#2C3E50",
    marginTop: 20,
    marginBottom: 10,
  },
  contentH3: {
    fontSize: 18,
    fontWeight: "600",
    color: "#2C3E50",
    marginTop: 16,
    marginBottom: 8,
  },
  contentP: {
    fontSize: 16,
    color: "#34495E",
    lineHeight: 26,
    marginBottom: 12,
  },
  listItem: {
    flexDirection: "row",
    marginBottom: 8,
  },
  bullet: {
    fontSize: 16,
    color: "#34495E",
    marginRight: 8,
  },
});
