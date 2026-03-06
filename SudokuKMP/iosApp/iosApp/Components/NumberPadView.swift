import SwiftUI

struct NumberPadView: View {
    let onNumberTap: (Int) -> Void
    let onErase: () -> Void
    let disabled: Bool

    var body: some View {
        VStack(spacing: 10) {
            // 1–5
            HStack(spacing: 8) {
                ForEach(1...5, id: \.self) { n in
                    numButton("\(n)") { onNumberTap(n) }
                }
            }
            // 6–9 + erase
            HStack(spacing: 8) {
                ForEach(6...9, id: \.self) { n in
                    numButton("\(n)") { onNumberTap(n) }
                }
                Button(action: onErase) {
                    Text("✕")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.red.opacity(0.5), lineWidth: 1.5))
                }
                .disabled(disabled)
            }
        }
        .padding(.horizontal, 16)
    }

    private func numButton(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(disabled ? Color.gray : Color.blue)
                .cornerRadius(8)
        }
        .disabled(disabled)
    }
}
