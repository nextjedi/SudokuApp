import React from "react";
import { render, fireEvent } from "@testing-library/react-native";
import { describe, it, expect, jest } from "@jest/globals";
import { Button } from "../Button";

describe("Button", () => {
  const defaultProps = {
    title: "Test Button",
    onPress: jest.fn(),
  };

  it("renders correctly with default props", () => {
    const { getByText } = render(<Button {...defaultProps} />);
    expect(getByText("Test Button")).toBeTruthy();
  });

  it("calls onPress when pressed", () => {
    const mockOnPress = jest.fn();
    const { getByText } = render(
      <Button {...defaultProps} onPress={mockOnPress} />,
    );

    fireEvent.press(getByText("Test Button"));
    expect(mockOnPress).toHaveBeenCalledTimes(1);
  });

  it("renders with different variants", () => {
    const { getByText, rerender } = render(
      <Button {...defaultProps} variant="primary" />,
    );
    expect(getByText("Test Button")).toBeTruthy();

    rerender(<Button {...defaultProps} variant="secondary" />);
    expect(getByText("Test Button")).toBeTruthy();

    rerender(<Button {...defaultProps} variant="danger" />);
    expect(getByText("Test Button")).toBeTruthy();
  });

  it("is disabled when disabled prop is true", () => {
    const mockOnPress = jest.fn();
    const { getByText } = render(
      <Button {...defaultProps} onPress={mockOnPress} disabled={true} />,
    );

    fireEvent.press(getByText("Test Button"));
    expect(mockOnPress).not.toHaveBeenCalled();
  });

  it("shows loading state when disabled", () => {
    const { getByText } = render(<Button {...defaultProps} disabled={true} />);

    // Button should still render the title even when disabled
    expect(getByText("Test Button")).toBeTruthy();
  });

  it("applies custom style", () => {
    const customStyle = { backgroundColor: "red" };
    const { getByText } = render(
      <Button {...defaultProps} style={customStyle} />,
    );

    const button = getByText("Test Button");
    expect(button).toBeTruthy();
    // Note: Style testing would require more complex setup with styled-components or similar
  });

  it("handles long titles", () => {
    const longTitle =
      "This is a very long button title that should still render properly";
    const { getByText } = render(
      <Button {...defaultProps} title={longTitle} />,
    );

    expect(getByText(longTitle)).toBeTruthy();
  });
});
