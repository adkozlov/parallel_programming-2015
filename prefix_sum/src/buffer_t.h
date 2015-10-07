#pragma once

#include <vector>

namespace details
{
    size_t round_up(size_t size)
    {
        size--;
        size |= size >> 1;
        size |= size >> 2;
        size |= size >> 4;
        size |= size >> 8;
        size |= size >> 16;
        size++;
        return size;
    }
}

template <typename T>
class buffer_t
{
public:
    buffer_t(size_t size, T default_value = T());
    buffer_t(const buffer_t &vector) = default;
    buffer_t& operator=(const buffer_t &vector) = default;

    size_t size() const;
    size_t capacity() const;

    operator T*();
    operator const T*() const;

    template <typename U>
    friend std::istream &operator>>(std::istream &stream, buffer_t<U> &vector);
    template <typename U>
    friend std::ostream &operator<<(std::ostream &stream, const buffer_t<U> &vector);

private:
    size_t size_;
    size_t capacity_;

    std::vector<T> vector_;
};

typedef buffer_t<float> buffer_f;

template <typename T>
size_t size_of(const buffer_t<T> &buffer)
{
    return sizeof(T) * buffer.size();
}

template <typename T>
size_t capacity_of(const buffer_t<T> &buffer)
{
    return sizeof(T) * buffer.capacity();
}

template <typename T>
buffer_t<T>::buffer_t(size_t size, T default_value)
    : size_(size)
    , capacity_(details::round_up(size_))
    , vector_(std::vector<T>(capacity_, default_value))
{
}

template <typename T>
size_t buffer_t<T>::size() const
{
    return size_;
}

template <typename T>
size_t buffer_t<T>::capacity() const
{
    return capacity_;
}

template <typename T>
buffer_t<T>::operator T*()
{
    return &vector_[0];
}

template <typename T>
buffer_t<T>::operator const T*() const
{
    return &vector_[0];
}

template <typename T>
std::istream &operator>>(std::istream &stream, buffer_t<T> &buffer)
{
    for (size_t i = 0; i < buffer.size_; ++i)
    {
        stream >> buffer.vector_[i];
    }
    return stream;
}

template <typename T>
std::ostream &operator<<(std::ostream &stream, const buffer_t<T> &buffer)
{
    stream << std::fixed << std::setprecision(3);
    for (size_t i = 0; i < buffer.size_ - 1; ++i)
    {
        stream << buffer.vector_[i] << " ";
    }
    stream << buffer.vector_[buffer.size_ - 1] << std::endl;
    return stream;
}