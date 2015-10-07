#pragma once

#include <vector>

template <typename T>
class matrix_t
{
public:
    matrix_t(size_t size, T default_value = T());
    matrix_t(const matrix_t &matrix) = default;
    matrix_t& operator=(const matrix_t &matrix) = default;

    size_t size() const;
    size_t buffer_size() const;

    operator T*();
    operator const T*() const;

    template <typename U>
    friend std::istream &operator>>(std::istream &stream, matrix_t<U> &matrix);
    template <typename U>
    friend std::ostream &operator<<(std::ostream &stream, const matrix_t<U> &matrix);

private:
    size_t size_;
    size_t buffer_size_;

    std::vector<T> vector_;
};

typedef matrix_t<float> matrix_f;

template <typename T>
matrix_t<T>::matrix_t(size_t size, T default_value)
    : size_(size)
    , buffer_size_(sizeof(T) * size_ * size_)
    , vector_(std::vector<T>(size * size, default_value))
{
}

template <typename T>
size_t matrix_t<T>::buffer_size() const
{
    return buffer_size_;
}

template <typename T>
size_t matrix_t<T>::size() const
{
    return size_;
}

template <typename T>
matrix_t<T>::operator T*()
{
    return &vector_[0];
}

template <typename T>
matrix_t<T>::operator const T*() const
{
    return &vector_[0];
}

template <typename T>
std::istream &operator>>(std::istream &stream, matrix_t<T> &matrix)
{
    auto size = matrix.size_;
    for (size_t i = 0; i < size; ++i)
    {
        for (size_t j = 0; j < size; ++j)
        {
            stream >> matrix.vector_[i * size + j];
        }
    }
    return stream;
}

template <typename T>
std::ostream &operator<<(std::ostream &stream, const matrix_t<T> &matrix)
{
    size_t size = matrix.size_;
    stream << std::fixed << std::setprecision(3);
    for (size_t i = 0; i < size; ++i)
    {
        for (size_t j = 0; j < size; ++j)
        {
            stream << matrix.vector_[i * size + j] << " ";
        }
        stream << std::endl;
    }
    return stream;
}